/**
 * Copyright (c) 2016-2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security;

import com.google.auto.service.AutoService;
import com.powsybl.commons.config.ConfigurationException;
import com.powsybl.commons.io.table.AsciiTableFormatterFactory;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.computation.Partition;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.ContingenciesProviders;
import com.powsybl.iidm.import_.Importers;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.config.SecurityAnalysisConfigurer;
import com.powsybl.security.config.SecurityAnalysisConfigurers;
import com.powsybl.security.converter.SecurityAnalysisResultExporters;
import com.powsybl.security.interceptors.SecurityAnalysisInterceptors;
import com.powsybl.security.json.JsonSecurityAnalysisParameters;
import com.powsybl.tools.Command;
import com.powsybl.tools.Tool;
import com.powsybl.tools.ToolOptions;
import com.powsybl.tools.ToolRunningContext;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Optional;

import static com.powsybl.tools.ToolConstants.TASK;
import static com.powsybl.tools.ToolConstants.TASK_COUNT;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
@AutoService(Tool.class)
public class SecurityAnalysisTool implements Tool {

    private static final String CASE_FILE_OPTION = "case-file";
    private static final String PARAMETERS_FILE = "parameters-file";
    private static final String LIMIT_TYPES_OPTION = "limit-types";
    private static final String OUTPUT_FILE_OPTION = "output-file";
    private static final String OUTPUT_FORMAT_OPTION = "output-format";
    private static final String CONTINGENCIES_FILE_OPTION = "contingencies-file";
    private static final String CONFIG_FILE_OPTION = "config-file";
    private static final String WITH_EXTENSIONS_OPTION = "with-extensions";
    private static final String EXTERNAL = "external";

    @Override
    public Command getCommand() {
        return new Command() {
            @Override
            public String getName() {
                return "security-analysis";
            }

            @Override
            public String getTheme() {
                return "Computation";
            }

            @Override
            public String getDescription() {
                return "Run security analysis";
            }

            @Override
            public Options getOptions() {
                Options options = new Options();
                options.addOption(Option.builder().longOpt(CASE_FILE_OPTION)
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
                options.addOption(Option.builder().longOpt(LIMIT_TYPES_OPTION)
                    .desc("limit type filter (all if not set)")
                    .hasArg()
                    .argName("LIMIT-TYPES")
                    .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FILE_OPTION)
                    .desc("the output path")
                    .hasArg()
                    .argName("FILE")
                    .build());
                options.addOption(Option.builder().longOpt(OUTPUT_FORMAT_OPTION)
                    .desc("the output format " + SecurityAnalysisResultExporters.getFormats())
                    .hasArg()
                    .argName("FORMAT")
                    .build());
                options.addOption(Option.builder().longOpt(CONTINGENCIES_FILE_OPTION)
                    .desc("the contingencies path")
                    .hasArg()
                    .argName("FILE")
                    .build());
                options.addOption(Option.builder().longOpt(CONFIG_FILE_OPTION)
                        .desc("the config file path")
                        .hasArg()
                        .argName("FILE")
                        .build());
                options.addOption(Option.builder().longOpt(WITH_EXTENSIONS_OPTION)
                    .desc("the extension list to enable")
                    .hasArg()
                    .argName("EXTENSIONS")
                    .build());
                options.addOption(Option.builder().longOpt(TASK_COUNT)
                        .desc("number of tasks used for parallelization")
                        .hasArg()
                        .argName("NTASKS")
                        .build());
                options.addOption(Option.builder().longOpt(TASK)
                        .desc("task identifier (task-index/task-count)")
                        .hasArg()
                        .argName("TASKID")
                        .build());
                options.addOption(Option.builder().longOpt(EXTERNAL)
                        .desc("external execution")
                        .build());
                return options;
            }

            @Override
            public String getUsageFooter() {
                return String.join(System.lineSeparator(),
                    "Allowed LIMIT-TYPES values are " + Arrays.toString(LimitViolationType.values()),
                    "Allowed EXTENSIONS values are " + SecurityAnalysisInterceptors.getExtensionNames()
                    );
            }
        };
    }


    @Override
    public void run(CommandLine line, ToolRunningContext context) throws Exception {

        ToolOptions options = new ToolOptions(line, context);

        Path caseFile = options.getPath(CASE_FILE_OPTION).orElseThrow(AssertionError::new);

        // Output file and output format
        Path outputFile = null;
        String format = null;
        if (options.hasOption(OUTPUT_FILE_OPTION)) {
            outputFile = options.getPath(OUTPUT_FILE_OPTION).orElseThrow(AssertionError::new);
            format = options.getValue(OUTPUT_FORMAT_OPTION)
                    .orElseThrow(() -> new ParseException("Missing required option: " + OUTPUT_FORMAT_OPTION));
        }

        // Contingencies file
        context.getOutputStream().println("Loading network '" + caseFile + "'");
        Network network = Importers.loadNetwork(caseFile);

        SecurityAnalysisInputs inputs = new SecurityAnalysisInputs();

        SecurityAnalysisParameters parameters = SecurityAnalysisParameters.load();
        options.getPath(PARAMETERS_FILE).ifPresent(f -> JsonSecurityAnalysisParameters.update(parameters, f));
        inputs.setParameters(parameters);

        options.getPath(CONTINGENCIES_FILE_OPTION)
                .map(f -> (ContingenciesProvider) ContingenciesProviders.newDefaultFactory().create(f))
                .ifPresent(inputs::setContingencies);


        Optional<Path> dslFile = options.getPath(CONFIG_FILE_OPTION);
        if (dslFile.isPresent()) {
            SecurityAnalysisConfigurer configurer = SecurityAnalysisConfigurers.getDefaultFactory()
                    .map(f -> f.create(dslFile.get()))
                    .orElseThrow(() -> new ConfigurationException("No security analysis configurer is defined, cannot handle config file."));

            configurer.configure(network, inputs);
        }


        // Start building security analysis, according to options
        // Common inputs
        SecurityAnalysisBuilder builder = new SecurityAnalysisBuilder()
                .network(network)
                .computationManager(context.getLongTimeExecutionComputationManager())
                .detector(inputs.getLimitViolationDetector());

        options.getValue(LIMIT_TYPES_OPTION).ifPresent(builder::limitViolationTypes);
        options.getValue(WITH_EXTENSIONS_OPTION).ifPresent(builder::extensions);

        // Computation distribution options
        if (options.hasOption(EXTERNAL)) {
            builder.external();
        }
        options.getInt(TASK_COUNT).ifPresent(builder::distributed);

        //For subtasks, we use the short time computation manager
        options.getValue(TASK, Partition::parse).ifPresent(p -> {
            builder.subTask(p);
            builder.computationManager(context.getShortTimeExecutionComputationManager());
        });

        SecurityAnalysis securityAnalysis = builder.build();

        String currentState = network.getStateManager().getWorkingStateId();

        SecurityAnalysisResult result = securityAnalysis.run(currentState, inputs.getParameters(), inputs.getContingenciesProvider()).join();

        if (!result.getPreContingencyResult().isComputationOk()) {
            context.getErrorStream().println("Pre-contingency state divergence");
        } else {
            if (outputFile != null) {
                context.getOutputStream().println("Writing results to '" + outputFile + "'");
                SecurityAnalysisResultExporters.export(result, outputFile, format);
            } else {
                // To avoid the closing of System.out
                Writer writer = new OutputStreamWriter(context.getOutputStream());
                Security.print(result, network, writer, new AsciiTableFormatterFactory(), TableFormatterConfig.load());
            }
        }
    }
}
