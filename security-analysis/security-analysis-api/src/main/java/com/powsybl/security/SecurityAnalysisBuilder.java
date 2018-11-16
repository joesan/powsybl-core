/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security;

import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.Partition;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.distributed.DistributedSecurityAnalysis;
import com.powsybl.security.distributed.ExternalSecurityAnalysis;
import com.powsybl.security.distributed.ExternalSecurityAnalysisConfig;
import com.powsybl.security.distributed.SecurityAnalysisTask;
import com.powsybl.security.interceptors.SecurityAnalysisInterceptor;
import com.powsybl.security.interceptors.SecurityAnalysisInterceptors;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Helper class to build a {@link SecurityAnalysis}, based on specified options,
 * in particular distribution options.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisBuilder {


    private boolean external = false;
    private Integer taskCount = null;
    private Partition part = null;
    private Network network = null;
    private ComputationManager computationManager = null;
    private List<String> extensions = new ArrayList<>();
    private Set<LimitViolationType> limitViolationTypes = EnumSet.allOf(LimitViolationType.class);
    private LimitViolationDetector detector = new DefaultLimitViolationDetector();

    public SecurityAnalysisBuilder external() {
        external = true;
        return this;
    }

    public SecurityAnalysisBuilder external(int taskCount) {
        this.external = true;
        this.taskCount = taskCount;
        return this;
    }

    public SecurityAnalysisBuilder distributed(int taskCount) {
        this.taskCount = taskCount;
        return this;
    }

    public SecurityAnalysisBuilder subTask(Partition part) {
        this.part = Objects.requireNonNull(part);
        return this;
    }

    public SecurityAnalysisBuilder network(Network network) {
        this.network = Objects.requireNonNull(network);
        return this;
    }

    public SecurityAnalysisBuilder computationManager(ComputationManager computationManager) {
        this.computationManager = Objects.requireNonNull(computationManager);
        return this;
    }

    public SecurityAnalysisBuilder limitViolationTypes(Collection<LimitViolationType> types) {
        Objects.requireNonNull(types);
        this.limitViolationTypes = EnumSet.copyOf(types);
        return this;
    }

    public SecurityAnalysisBuilder limitViolationTypes(String types) {
        Objects.requireNonNull(types);
        this.limitViolationTypes = Arrays.stream(types.split(","))
                .map(LimitViolationType::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(LimitViolationType.class)));
        return this;
    }

    public SecurityAnalysisBuilder detector(LimitViolationDetector detector) {
        this.detector = Objects.requireNonNull(detector);
        return this;
    }

    public SecurityAnalysisBuilder extensions(String commaSeparatedExtensions) {
        Objects.requireNonNull(commaSeparatedExtensions);
        Arrays.stream(commaSeparatedExtensions.split(","))
                        .filter(ext -> !ext.isEmpty())
                        .forEach(this.extensions::add);
        return this;
    }

    public SecurityAnalysis build() {

        if (external) {
            ExternalSecurityAnalysisConfig config = ExternalSecurityAnalysisConfig.load();
            return new ExternalSecurityAnalysis(config, network, computationManager, extensions, taskCount);
        }

        if (taskCount != null) {
            ExternalSecurityAnalysisConfig config = ExternalSecurityAnalysisConfig.load();
            return new DistributedSecurityAnalysis(config, network, computationManager, extensions, taskCount);
        }

        Set<SecurityAnalysisInterceptor> interceptors = extensions.stream()
                .map(SecurityAnalysisInterceptors::createInterceptor)
                .collect(Collectors.toSet());

        LimitViolationFilter limitViolationFilter = LimitViolationFilter.load();
        limitViolationFilter.setViolationTypes(limitViolationTypes);

        SecurityAnalysis securityAnalysis = SecurityAnalysisFactories.newDefaultFactory()
                .create(network, detector, limitViolationFilter, computationManager, 0);
        interceptors.forEach(securityAnalysis::addInterceptor);

        if (part != null) {
            return new SecurityAnalysisTask(securityAnalysis, part);
        }

        return securityAnalysis;
    }

}
