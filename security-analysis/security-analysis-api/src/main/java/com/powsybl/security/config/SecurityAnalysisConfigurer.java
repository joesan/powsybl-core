/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.config;

import com.powsybl.iidm.network.Network;
import com.powsybl.security.SecurityAnalysisInputs;

/**
 * A configurer which may be called before the execution of a security analysis,
 * in order to customize its {@link SecurityAnalysisInputs}, in particular
 * contingencies and limit violations detection.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public interface SecurityAnalysisConfigurer {

    /**
     * Configure the security analysis inputs.
     *
     * @param network          The network on which computation will be executed.
     * @param inputs           The inputs to be configured.
     */
    void configure(Network network, SecurityAnalysisInputs inputs);

}
