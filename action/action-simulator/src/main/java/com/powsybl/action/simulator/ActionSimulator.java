/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.simulator;

import com.powsybl.action.dsl.ActionDb;
import com.powsybl.commons.PowsyblException;
import com.powsybl.commons.io.table.TableFormatterConfig;
import com.powsybl.loadflow.LoadFlowParameters;

import java.util.List;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public interface ActionSimulator {

    String getName();

    void start(ActionDb actionDb, List<String> contingencyIds);

    void start(ActionDb actionDb, String... contingencyIds);

    default void start(ActionDb actionDb, List<String> contingencyIds, LoadFlowParameters loadFlowParameters) {
        throw new PowsyblException("Not implemented");
    }

    default void start(ActionDb actionDb, List<String> contingencyIds, LoadFlowParameters loadFlowParameters,
               TableFormatterConfig tableFormatterConfig) {
        throw new PowsyblException("Not implemented");
    }
}
