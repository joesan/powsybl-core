/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.dsl;

import com.powsybl.commons.util.ServiceLoaderCache;
import com.powsybl.dsl.DslLoader;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.config.SecurityAnalysisConfigurer;
import com.powsybl.security.SecurityAnalysisInputs;
import groovy.lang.Binding;
import groovy.lang.GroovyCodeSource;
import groovy.lang.GroovyShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisDslConfigurer implements SecurityAnalysisConfigurer {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityAnalysisDslConfigurer.class);

    private final GroovyCodeSource dslSrc;

    public SecurityAnalysisDslConfigurer(GroovyCodeSource dslSrc) {
        this.dslSrc = Objects.requireNonNull(dslSrc);
    }

    public SecurityAnalysisDslConfigurer(String script) {
        this(new GroovyCodeSource(Objects.requireNonNull(script), "script", GroovyShell.DEFAULT_CODE_BASE));
    }

    public SecurityAnalysisDslConfigurer(InputStream input) {
        this(new GroovyCodeSource(new InputStreamReader(Objects.requireNonNull(input),
                StandardCharsets.UTF_8), "script", GroovyShell.DEFAULT_CODE_BASE));
    }

    public SecurityAnalysisDslConfigurer(Path file) {
        Objects.requireNonNull(file);
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            this.dslSrc = new GroovyCodeSource(reader, "script", GroovyShell.DEFAULT_CODE_BASE);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void configure(Network network, SecurityAnalysisInputs inputs) {

        LOGGER.debug("Starting configuration of security analysis inputs, based on groovy DSL.");

        Binding binding = new Binding();

        new ServiceLoaderCache<>(SecurityAnalysisDsl.class).getServices()
                .forEach(dsl -> dsl.loadDsl(binding, inputs));

        LOGGER.debug("Evaluating security analysis configuration DSL file.");
        DslLoader.createShell(binding).evaluate(dslSrc);
    }

}
