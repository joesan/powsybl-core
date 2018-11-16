/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.config;

import com.google.auto.service.AutoService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisConfigurersTest {

    @AutoService(SecurityAnalysisConfigurerFactory.class)
    public static class TestConfigurerFactory implements SecurityAnalysisConfigurerFactory {

        @Override
        public SecurityAnalysisConfigurer create(Path file) {
            return null;
        }

        @Override
        public SecurityAnalysisConfigurer create(InputStream inputStream) {
            return null;
        }
    }

    @Test
    public void test() {
        Assertions.assertThat(SecurityAnalysisConfigurers.getDefaultFactory())
                .isPresent()
                .get()
                .isInstanceOf(TestConfigurerFactory.class);
    }
}
