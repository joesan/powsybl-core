/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security;

import com.google.common.jimfs.Configuration;
import com.google.common.jimfs.Jimfs;
import com.powsybl.commons.config.InMemoryPlatformConfig;
import com.powsybl.commons.config.PlatformConfig;
import com.powsybl.computation.ComputationManager;
import com.powsybl.computation.Partition;
import com.powsybl.iidm.network.Network;
import com.powsybl.security.distributed.DistributedSecurityAnalysis;
import com.powsybl.security.distributed.ExternalSecurityAnalysis;
import com.powsybl.security.distributed.SecurityAnalysisTask;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.FileSystem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class SecurityAnalysisBuilderTest {


    private FileSystem fileSystem;
    private SecurityAnalysisBuilder builder;
    private SecurityAnalysis securityAnalysis;

    @Before
    public void setUp() {
        fileSystem = Jimfs.newFileSystem(Configuration.unix());
        PlatformConfig config = new InMemoryPlatformConfig(fileSystem);
        securityAnalysis = mock(SecurityAnalysis.class);
        SecurityAnalysisFactory factory = new SecurityAnalysisFactory() {
            @Override
            public SecurityAnalysis create(Network network, ComputationManager computationManager, int priority) {
                return securityAnalysis;
            }

            @Override
            public SecurityAnalysis create(Network network, LimitViolationFilter filter, ComputationManager computationManager, int priority) {
                return securityAnalysis;
            }

            @Override
            public SecurityAnalysis create(Network network, LimitViolationDetector detector, LimitViolationFilter filter, ComputationManager computationManager, int priority) {
                return securityAnalysis;
            }
        };

        builder = new SecurityAnalysisBuilder(config, () -> factory);
    }

    @After
    public void tearDown() throws IOException {
        fileSystem.close();
    }

    @Test
    public void specificBuilds() {
        builder.network(mock(Network.class))
                .computationManager(mock(ComputationManager.class));

        assertNotNull(builder.buildExternal());
        assertNotNull(builder.buildExternal(12));
        assertNotNull(builder.buildDistributed(12));
        assertNotNull(builder.buildTask(Partition.parse("1/2")));
    }

    @Test
    public void build() {
        builder.network(mock(Network.class))
                .computationManager(mock(ComputationManager.class));

        assertSame(securityAnalysis, builder.build());
        assertSame(securityAnalysis, builder.buildDefault());

        builder.distributed(12);
        assertThat(builder.build()).isInstanceOf(DistributedSecurityAnalysis.class);

        builder.external();
        assertThat(builder.build()).isInstanceOf(ExternalSecurityAnalysis.class);
    }

    @Test
    public void subTask() {
        builder.network(mock(Network.class))
                .computationManager(mock(ComputationManager.class))
                .subTask(Partition.parse("1/2"));
        assertThat(builder.build()).isInstanceOf(SecurityAnalysisTask.class);
    }

    @Test
    public void exception1() {
        assertThatNullPointerException().isThrownBy(() -> builder.build());
        assertThatNullPointerException().isThrownBy(() -> builder.network(mock(Network.class)).build());
        assertThatNullPointerException().isThrownBy(() -> builder.network(null));
        assertThatNullPointerException().isThrownBy(() -> builder.computationManager(null));
        assertThatNullPointerException().isThrownBy(() -> builder.subTask(null));
    }

    @Test
    public void exception2() {
        assertThatNullPointerException().isThrownBy(() -> builder.computationManager(mock(ComputationManager.class)).build());
    }

}
