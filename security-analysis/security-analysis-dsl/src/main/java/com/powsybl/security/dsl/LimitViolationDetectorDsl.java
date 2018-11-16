/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.dsl;

import com.google.auto.service.AutoService;
import com.powsybl.security.SecurityAnalysisInputs;
import groovy.lang.Binding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
@AutoService(SecurityAnalysisDsl.class)
public class LimitViolationDetectorDsl implements SecurityAnalysisDsl {

    private static final Logger LOGGER = LoggerFactory.getLogger(LimitViolationDetectorDsl.class);

    private static void bindDetector(SecurityAnalysisInputs inputs, LimitFactors factors) {
        inputs.setDetector(new LimitViolationDetectorWithFactors(factors));
    }

    @Override
    public void loadDsl(Binding binding, SecurityAnalysisInputs inputs) {
        LOGGER.debug("Loading limits violation detector DSL.");
        LimitFactorsLoader.loadDsl(binding, f -> bindDetector(inputs, f));
    }
}
