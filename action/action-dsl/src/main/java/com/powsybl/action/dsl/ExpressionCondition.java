/**
 * Copyright (c) 2017, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.action.dsl;

import com.powsybl.action.dsl.ast.ExpressionNode;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * @author Geoffroy Jamgotchian <geoffroy.jamgotchian at rte-france.com>
 */
public class ExpressionCondition implements Condition {

    private final ExpressionNode node;

    private final List<String> hypoContingencies;

    public ExpressionCondition(ExpressionNode node) {
        this(node, Collections.emptyList());
    }

    public ExpressionCondition(ExpressionNode node, List<String> hypoContingencies) {
        this.node = Objects.requireNonNull(node);
        this.hypoContingencies = Objects.requireNonNull(hypoContingencies);
    }

    @Override
    public ConditionType getType() {
        return ConditionType.EXPRESSION;
    }

    @Override
    public List<String> getHypoContingencies() {
        return Collections.unmodifiableList(hypoContingencies);
    }

    public ExpressionNode getNode() {
        return node;
    }
}
