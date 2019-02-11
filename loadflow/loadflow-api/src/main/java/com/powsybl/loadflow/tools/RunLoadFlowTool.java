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
import com.powsybl.iidm.network.Bus;
import com.powsybl.iidm.network.Generator;
import com.powsybl.loadflow.LoadFlowParameters;
import com.powsybl.loadflow.json.JsonLoadFlowParameters;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolRunningContext;
import com.powsybl.iidm.export.Exporters;
import com.powsybl.iidm.import_.ImportConfig;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
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
    private static final String COMPARISON_FOLDER = "comparison-folder";

    private static final String ANGLE = ".angle";
    private static final String V = ".v";
    private static final String P = ".p";
    private static final String Q = ".q";

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
                options.addOption(Option.builder().longOpt(COMPARISON_FOLDER)
                        .desc("path of folder where comparison files are generated")
                        .hasArg()
                        .argName("FOLDER")
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

        Map<String, Double> expected = compareBeforeLoadflow(line, network);

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

        compareAfterLoadflow(expected, line, network);

        // exports the modified network to the filesystem, if requested
        if (outputCaseFile != null) {
            String outputCaseFormat = line.getOptionValue(OUTPUT_CASE_FORMAT);
            Exporters.export(outputCaseFormat, network, new Properties(), outputCaseFile);
        }
    }

    private static Map<String, Double> compareBeforeLoadflow(CommandLine line, Network network) throws IOException {
        if (line.hasOption(COMPARISON_FOLDER)) {
            Map<String, Double> expected = new HashMap<>();
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(line.getOptionValue(COMPARISON_FOLDER) + "expected_buses.csv"))) {
                writer.write("ID;angle;v\n");
                for (Bus bus : network.getBusBreakerView().getBuses()) {
                    String id = bus.getId();
                    writer.write(id + ";" + bus.getAngle() + ";" + bus.getV() + "\n");
                    expected.put(id + ANGLE, bus.getAngle());
                    expected.put(id + V, bus.getV());
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(line.getOptionValue(COMPARISON_FOLDER) + "expected_generators.csv"))) {
                writer.write("ID;p;q\n");
                for (Generator generator : network.getGenerators()) {
                    String id = generator.getId();
                    double p = generator.getTerminal().getP();
                    double q = generator.getTerminal().getQ();
                    writer.write(id + ";" + p + ";" + q + "\n");
                    expected.put(id + P, p);
                    expected.put(id + Q, q);
                }
            }
            return expected;
        }
        return null;
    }

    private static void compareAfterLoadflow(Map<String, Double> expected, CommandLine line, Network network) throws IOException {
        if (line.hasOption(COMPARISON_FOLDER)) {
            Objects.requireNonNull(expected);
            Map<String, Double> diff = new HashMap<>();
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(line.getOptionValue(COMPARISON_FOLDER) + "actual_buses.csv"))) {
                writer.write("ID;angle;v\n");
                for (Bus bus : network.getBusBreakerView().getBuses()) {
                    double angle = bus.getAngle();
                    double v = bus.getV();
                    String id = bus.getId();
                    writer.write(id + ";" + angle + ";" + v + "\n");
                    if (!expected.get(id + ANGLE).equals(angle)) {
                        diff.put(id + ANGLE, angle);
                    }
                    if (!expected.get(id + V).equals(v)) {
                        diff.put(id + V, v);
                    }
                }
            }
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(line.getOptionValue(COMPARISON_FOLDER) + "actual_generators.csv"))) {
                writer.write("ID;p;q\n");
                for (Generator generator : network.getGenerators()) {
                    String id = generator.getId();
                    double p = generator.getTerminal().getP();
                    double q = generator.getTerminal().getQ();
                    writer.write(id + ";" + p + ";" + q + "\n");
                    if (!expected.get(id + P).equals(p)) {
                        diff.put(id + P, p);
                    }
                    if (!expected.get(id + Q).equals(q)) {
                        diff.put(id + Q, q);
                    }
                }
            }
            writeDiff(expected, diff, line.getOptionValue(COMPARISON_FOLDER) + "difference.csv");
        }
    }

    private static void writeDiff(Map<String, Double> expected, Map<String, Double> diff, String file) throws IOException {
        if (!diff.isEmpty()) {
            try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(file))) {
                writer.write("ID.attribute;expected;actual\n");
                for (Map.Entry<String, Double> entry : diff.entrySet()) {
                    String key = entry.getKey();
                    writer.write(key + ";" + expected.get(key) + ";" + entry.getValue() + "\n");
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
