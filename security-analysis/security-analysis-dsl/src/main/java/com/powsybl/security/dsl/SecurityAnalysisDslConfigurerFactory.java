/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.dsl;

import com.google.auto.service.AutoService;
import com.powsybl.security.config.SecurityAnalysisConfigurer;
import com.powsybl.security.config.SecurityAnalysisConfigurerFactory;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
@AutoService(SecurityAnalysisConfigurerFactory.class)
public class SecurityAnalysisDslConfigurerFactory implements SecurityAnalysisConfigurerFactory {


    @Override
    public SecurityAnalysisConfigurer create(Path file) {
        return new SecurityAnalysisDslConfigurer(file);
    }

    @Override
    public SecurityAnalysisConfigurer create(InputStream inputStream) {
        return new SecurityAnalysisDslConfigurer(inputStream);
    }
}
