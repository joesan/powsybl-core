/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security;

import com.powsybl.commons.config.ConfigurationException;
import com.powsybl.contingency.ContingenciesProvider;
import com.powsybl.contingency.ContingenciesProviders;
import com.powsybl.contingency.EmptyContingencyListProvider;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisInputsTest {

    @Test
    public void test() {

        SecurityAnalysisInputs inputs = new SecurityAnalysisInputs();

        assertThat(inputs.getContingenciesProvider())
                .isNotNull()
                .isInstanceOf(EmptyContingencyListProvider.class);
        assertThat(inputs.getLimitViolationDetector())
                .isNotNull()
                .isInstanceOf(DefaultLimitViolationDetector.class);

        SecurityAnalysisParameters params = new SecurityAnalysisParameters();
        ContingenciesProvider provider = ContingenciesProviders.emptyProvider();
        LimitViolationDetector detector = new DefaultLimitViolationDetector();

        inputs.setParameters(params);
        inputs.setContingencies(provider);
        inputs.setDetector(detector);

        assertThat(inputs.getParameters())
                .isNotNull()
                .isSameAs(params);
        assertThat(inputs.getContingenciesProvider())
                .isNotNull()
                .isSameAs(provider);
        assertThat(inputs.getLimitViolationDetector())
                .isNotNull()
                .isSameAs(detector);

        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> inputs.setParameters(params));
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> inputs.setContingencies(provider));
        assertThatExceptionOfType(ConfigurationException.class)
                .isThrownBy(() -> inputs.setDetector(detector));
    }

}
