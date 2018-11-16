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

import java.util.Objects;

/**
 *
 * Input data for {@link SecurityAnalysis}.
 * May be defined explicitly or defined by a custom configurer.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisInputs {

    private LimitViolationDetector detector;
    private ContingenciesProvider contingencies;
    private SecurityAnalysisParameters parameters;

    /**
     * Get specified {@link SecurityAnalysisParameters}, or load them from config if not set.
     */
    public SecurityAnalysisParameters getParameters() {
        return parameters != null ? parameters : SecurityAnalysisParameters.load();
    }


    /**
     * Get specified {@link ContingenciesProvider}, or an empty one if not set.
     */
    public ContingenciesProvider getContingenciesProvider() {
        return contingencies != null ? contingencies : ContingenciesProviders.emptyProvider();
    }

    /**
     * Get specified {@link LimitViolationDetector}, or {@link DefaultLimitViolationDetector} if not set.
     */
    public LimitViolationDetector getLimitViolationDetector() {
        return detector != null ? detector : new DefaultLimitViolationDetector();
    }

    /**
     * Define limit violation detectors. May be manually set or set by a configurer,
     * but an exception will be raised in case method is called several times.
     */
    public SecurityAnalysisInputs setDetector(LimitViolationDetector detector) {
        Objects.requireNonNull(detector);
        if (this.detector != null) {
            throw new ConfigurationException("A limit violation detector has already been defined.");
        }
        this.detector = detector;
        return this;
    }

    /**
     * Define contingencies. May be manually set or set by a configurer,
     * but an exception will be raised in case method is called several times.
     */
    public SecurityAnalysisInputs setContingencies(ContingenciesProvider contingencies) {
        Objects.requireNonNull(contingencies);
        if (this.contingencies != null) {
            throw new ConfigurationException("A contingencies provider has already been defined.");
        }
        this.contingencies = contingencies;
        return this;
    }

    /**
     * Define parameters. May be manually set or set by a configurer,
     * but an exception will be raised in case method is called several times.
     */
    public SecurityAnalysisInputs setParameters(SecurityAnalysisParameters parameters) {
        Objects.requireNonNull(parameters);
        if (this.parameters != null) {
            throw new ConfigurationException("Parameters have already been defined.");
        }
        this.parameters = parameters;
        return this;
    }

}
