/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.config;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * Factory service for {@link SecurityAnalysisConfigurer}s.
 * In order to implement your own configurer, you will need to provide
 * an implementation of that interface as a service
 * (possibly using @{@link com.google.auto.service.AutoService}).
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface SecurityAnalysisConfigurerFactory {

    SecurityAnalysisConfigurer create(Path file);
    SecurityAnalysisConfigurer create(InputStream inputStream);
}
