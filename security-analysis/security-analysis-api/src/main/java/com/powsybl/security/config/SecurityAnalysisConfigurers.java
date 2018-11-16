/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.config;

import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.util.ServiceLoaderCache;

import java.util.List;
import java.util.Optional;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public final class SecurityAnalysisConfigurers {

    private SecurityAnalysisConfigurers() {

    }

    /**
     * If exactly one {@link SecurityAnalysisConfigurerFactory} service is present at runtime,
     * returns the corresponding instance.
     * If none is defined, return empty.
     * Throws an exception in case several services are present at runtime.
     */
    public static Optional<SecurityAnalysisConfigurerFactory> getDefaultFactory() {
        List<SecurityAnalysisConfigurerFactory> configurers = new ServiceLoaderCache<>(SecurityAnalysisConfigurerFactory.class).getServices();

        if (configurers.isEmpty()) {
            return Optional.empty();
        }

        if (configurers.size() > 1) {
            throw new PowsyblException("Multiple security analysis configurers detected, only 1 may be provided.");
        }

        return Optional.of(configurers.get(0));
    }


}
