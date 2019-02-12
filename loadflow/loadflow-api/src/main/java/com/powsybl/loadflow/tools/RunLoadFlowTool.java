/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.loadflow.tools;

import com.google.auto.service.AutoService;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.config.ComponentDefaultConfig;
import com.powsybl.commons.io.table.*;
import com.powsybl.iidm.network.*;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.loadflow.LoadFlow;
import com.powsybl.loadflow.LoadFlowFactory;
import com.powsybl.loadflow.LoadFlowResult;
import com.powsybl.loadflow.json.LoadFlowResultSerializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;

/**
 * @author Christian Biasuzzi <christian.biasuzzi@techrain.it>
 */
@AutoService(Tool.class)
public class RunLoadFlowTool implements Tool {

    private static final String CASE_FILE = "case-file";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String OUTPUT_FILE = "output-file";
    private static final String OUTPUT_FORMAT = "output-format";
    private static final String SKIP_POSTPROC = "skip-postproc";
    private static final String OUTPUT_CASE_FORMAT = "output-case-format";
    private static final String OUTPUT_CASE_FILE = "output-case-file";
    private static final String COMPARISON_FILE = "comparison-file";

    private static final String ANGLE = ".angle";
    private static final String V = ".v";
    private static final String P = ".p";
    private static final String Q = ".q";
    private static final String P1 = ".p1";
    private static final String P2 = ".p2";
    private static final String P3 = ".p3";
    private static final String Q1 = ".q1";
    private static final String Q2 = ".q2";
    private static final String Q3 = ".q3";

    private enum Format {
        CSV,
        JSON
    }

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "loadflow";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run loadflow";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE)
                        .desc("the case path")
                        .hasArg()
                        .argName("FILE")
                        .required()
                        .build());
                options.addOption(Option.builder().longOpt(PARAMETERS_FILE)
                        .desc("loadflow parameters as JSON file")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE)
                        .desc("loadflow results output path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FORMAT)
                        .desc("loadflow results output format " + Arrays.toString(Format.values()))
                        .hasArg()
                        .argName("FORMAT")
                        .build());
                options.addOption(Option.builder().longOpt(SKIP_POSTPROC)
                        .desc("skip network importer post processors (when configured)")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_CASE_FORMAT)
                        .desc("modified network output format " + Exporters.getFormats())
                        .hasArg()
                        .argName("CASEFORMAT")
                        .build());
                options.addOption(Option.builder().longOpt(OUTPUT_CASE_FILE)
                        .desc("modified network base name")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(COMPARISON_FILE)
                        .desc("path of file where comparison files is generated")
                        .hasArg()
                        .argName("FILE")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return null;
            }
        };
    }

    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {
        Path caseFile = context.getFileSystem().getPath(line.getOptionValue(CASE_FILE));
        boolean skipPostProc = line.hasOption(SKIP_POSTPROC);
        Path outputFile = null;
        Format format = null;
        Path outputCaseFile = null;
        ComponentDefaultConfig defaultConfig = ComponentDefaultConfig.load();

        ImportConfig importConfig = (!skipPostProc) ? ImportConfig.load() : new ImportConfig();
        // process a single network: output-file/output-format options available
        if (line.hasOption(OUTPUT_FILE)) {
            outputFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_FILE));
            if (!line.hasOption(OUTPUT_FORMAT)) {
                throw new ParseException("Missing required option: " + OUTPUT_FORMAT);
            }
            format = Format.valueOf(line.getOptionValue(OUTPUT_FORMAT));
        }

        if (line.hasOption(OUTPUT_CASE_FILE)) {
            outputCaseFile = context.getFileSystem().getPath(line.getOptionValue(OUTPUT_CASE_FILE));
            if (!line.hasOption(OUTPUT_CASE_FORMAT)) {
                throw new ParseException("Missing required option: " + OUTPUT_CASE_FORMAT);
            }
        }

        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile, context.getShortTimeExecutionComputationManager(), importConfig, null);
        if (network == null) {
            throw new PowsyblException("Case '" + caseFile + "' not found");
        }

        Map<String, Double> expected = null;
        if (line.hasOption(COMPARISON_FILE)) {
            expected = loadExpectedResults(network);
        }

        LoadFlow loadFlow = defaultConfig.newFactoryImpl(LoadFlowFactory.class).create(network, context.getShortTimeExecutionComputationManager(), 0);

        LoadFlowParameters params = LoadFlowParameters.load();
        if (line.hasOption(PARAMETERS_FILE)) {
            Path parametersFile = context.getFileSystem().getPath(line.getOptionValue(PARAMETERS_FILE));
            JsonLoadFlowParameters.update(params, parametersFile);
        }

        LoadFlowResult result = loadFlow.run(network.getVariantManager().getWorkingVariantId(), params).join();

        if (outputFile != null) {
            exportResult(result, context, outputFile, format);
        } else {
            printResult(result, context);
        }

        if (line.hasOption(COMPARISON_FILE)) {
            Map<String, Double> diff = compare(expected, network);
            writeDiff(expected, diff, line.getOptionValue(COMPARISON_FILE));
        }

        // exports the modified network to the filesystem, if requested
        if (outputCaseFile != null) {
            String outputCaseFormat = line.getOptionValue(OUTPUT_CASE_FORMAT);
            Exporters.export(outputCaseFormat, network, new Properties(), outputCaseFile);
        }
    }

    private Map<String, Double> loadExpectedResults(Network network) {
        Objects.requireNonNull(network);
        Map<String, Double> expected = new HashMap<>();
        loadBuses(Collections.emptyMap(), expected, network);
        loadInjections(Collections.emptyMap(), expected, network, Network::getGenerators);
        loadInjections(Collections.emptyMap(), expected, network, Network::getShuntCompensators);
        loadInjections(Collections.emptyMap(), expected, network, Network::getLoads);
        loadInjections(Collections.emptyMap(), expected, network, Network::getStaticVarCompensators);
        loadInjections(Collections.emptyMap(), expected, network, Network::getLccConverterStations);
        loadInjections(Collections.emptyMap(), expected, network, Network::getVscConverterStations);
        loadInjections(Collections.emptyMap(), expected, network, Network::getBusbarSections);
        loadInjections(Collections.emptyMap(), expected, network, Network::getDanglingLines);
        loadBranches(Collections.emptyMap(), expected, network, Network::getLines);
        loadBranches(Collections.emptyMap(), expected, network, Network::getTwoWindingsTransformers);
        loadThreeWindingsTransformers(Collections.emptyMap(), expected, network);
        return expected;
    }

    private Map<String, Double> compare(Map<String, Double> expected, Network network) {
        Objects.requireNonNull(expected);
        Objects.requireNonNull(network);
        Map<String, Double> diff = new HashMap<>();
        loadBuses(expected, diff, network);
        loadInjections(expected, diff, network, Network::getGenerators);
        loadInjections(expected, diff, network, Network::getShuntCompensators);
        loadInjections(expected, diff, network, Network::getLoads);
        loadInjections(expected, diff, network, Network::getStaticVarCompensators);
        loadInjections(expected, diff, network, Network::getLccConverterStations);
        loadInjections(expected, diff, network, Network::getVscConverterStations);
        loadInjections(expected, diff, network, Network::getBusbarSections);
        loadInjections(expected, diff, network, Network::getDanglingLines);
        loadBranches(expected, diff, network, Network::getLines);
        loadBranches(expected, diff, network, Network::getTwoWindingsTransformers);
        loadThreeWindingsTransformers(expected, diff, network);
        return diff;
    }

    private void loadBuses(Map<String, Double> previous, Map<String, Double> map, Network network) {
        Objects.requireNonNull(previous);
        Objects.requireNonNull(map);
        Objects.requireNonNull(network);
        for (Bus bus : network.getBusBreakerView().getBuses()) {
            String id = bus.getId();
            fillDiffMap(id + ANGLE, bus.getAngle(), previous, map);
            fillDiffMap(id + V, bus.getV(), previous, map);
        }
    }

    private <T extends Injection<T>> void loadInjections(Map<String, Double> previous, Map<String, Double> map, Network network, Function<Network, Iterable<T>> function) {
        for (T t : function.apply(network)) {
            String id = t.getId();
            fillDiffMap(id + P, t.getTerminal().getP(), previous, map);
            fillDiffMap(id + Q, t.getTerminal().getQ(), previous, map);
        }
    }

    private <T extends Branch<T>> void loadBranches(Map<String, Double> previous, Map<String, Double> map, Network network, Function<Network, Iterable<T>> function) {
        for (T t : function.apply(network)) {
            String id = t.getId();
            fillDiffMap(id + P1, t.getTerminal1().getP(), previous, map);
            fillDiffMap(id + P2, t.getTerminal2().getP(), previous, map);
            fillDiffMap(id + Q1, t.getTerminal1().getQ(), previous, map);
            fillDiffMap(id + Q2, t.getTerminal2().getQ(), previous, map);
        }
    }

    private void loadThreeWindingsTransformers(Map<String, Double> previous, Map<String, Double> map, Network network) {
        for (ThreeWindingsTransformer t : network.getThreeWindingsTransformers()) {
            String id = t.getId();
            fillDiffMap(id + P1, t.getLeg1().getTerminal().getP(), previous, map);
            fillDiffMap(id + P2, t.getLeg2().getTerminal().getP(), previous, map);
            fillDiffMap(id + P3, t.getLeg3().getTerminal().getP(), previous, map);
            fillDiffMap(id + Q1, t.getLeg1().getTerminal().getQ(), previous, map);
            fillDiffMap(id + Q2, t.getLeg2().getTerminal().getQ(), previous, map);
            fillDiffMap(id + Q3, t.getLeg3().getTerminal().getQ(), previous, map);
        }
    }

    private void fillDiffMap(String key, double value, Map<String, Double> previous, Map<String, Double> map) {
        if (previous.get(key) == null || !previous.get(key).equals(value)) {
            map.put(key, value);
        } else {
            previous.remove(key);
        }
    }

    private void writeDiff(Map<String, Double> expected, Map<String, Double> diff, String file) throws IOException {
        Objects.requireNonNull(file);
        if (!diff.isEmpty() || !expected.isEmpty()) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file))) {
                try (TableFormatter formatter = new CsvTableFormatter(writer,
                        "comparison before and after computation",
                        TableFormatterConfig.load(),
                        new Column("ID.attribute"),
                        new Column("expected"),
                        new Column("actual"))) {
                    for (Map.Entry<String, Double> entry : diff.entrySet()) {
                        String key = entry.getKey();
                        formatter.writeCell(key);
                        formatter.writeCell(expected.get(key));
                        formatter.writeCell(entry.getValue());
                        expected.remove(key);
                    }
                    for (Map.Entry<String, Double> entry : expected.entrySet()) {
                        String key = entry.getKey();
                        formatter.writeCell(key);
                        formatter.writeCell(expected.get(key));
                        formatter.writeCell("null");
                    }
                }
            }
        }
    }

    private void printLoadFlowResult(LoadFlowResult result, Path outputFile, TableFormatterFactory formatterFactory,
                                     TableFormatterConfig formatterConfig) {
        try (Writer writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8)) {
            printLoadFlowResult(result, writer, formatterFactory, formatterConfig);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printLoadFlowResult(LoadFlowResult result, Writer writer, TableFormatterFactory formatterFactory,
                                     TableFormatterConfig formatterConfig) {
        try (TableFormatter formatter = formatterFactory.create(writer,
                "loadflow results",
                formatterConfig,
                new Column("Result"),
                new Column("Metrics"))) {
            formatter.writeCell(result.isOk());
            formatter.writeCell(result.getMetrics().toString());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void printResult(LoadFlowResult result, ToolRunningContext context) {
        Writer writer = new OutputStreamWriter(context.getOutputStream());

        AsciiTableFormatterFactory asciiTableFormatterFactory = new AsciiTableFormatterFactory();
        printLoadFlowResult(result, writer, asciiTableFormatterFactory, TableFormatterConfig.load());
    }


    private void exportResult(LoadFlowResult result, ToolRunningContext context, Path outputFile, Format format) {
        context.getOutputStream().println("Writing results to '" + outputFile + "'");
        switch (format) {
            case CSV:
                CsvTableFormatterFactory csvTableFormatterFactory = new CsvTableFormatterFactory();
                printLoadFlowResult(result, outputFile, csvTableFormatterFactory, TableFormatterConfig.load());
                break;

            case JSON:
                LoadFlowResultSerializer.write(result, outputFile);
                break;
        }
    }
}
