/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.dsl;

import com.google.common.collect.ImmutableSet;

import java.util.Collection;
import java.util.Set;

/**
 * Provides implementations of {@link LimitMatcher}.
 *
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
final class LimitMatchers {

    private LimitMatchers() {
    }

    /**
     * A {@link LimitMatcher} which matches temporary limits.
     */
    public static final LimitMatcher TEMPORARY = (b, s, l, c) -> l != null;


    /**
     * A {@link LimitMatcher} which matches permanent limits.
     */
    public static final LimitMatcher PERMANENT = (b, s, l, c) -> l == null;

    /**
     * A {@link LimitMatcher} which matches post-contingency contexts.
     */
    public static final LimitMatcher ANY_CONTINGENCY = (b, s, l, c) -> c != null;

    /**
     * A {@link LimitMatcher} which matches N situation context.
     */
    public static final LimitMatcher N_SITUATION = (b, s, l, c) -> c == null;



    /**
     * A {@link LimitMatcher} which matches the branch with the specified ID.
     */
    static LimitMatcher branch(String id) {
        return (b, s, l, c) -> b.getId().equals(id);
    }

    /**
     * A {@link LimitMatcher} which matches the branches with the specified IDs.
     */
    static LimitMatcher branches(Collection<String> ids) {
        Set<String> set = ImmutableSet.copyOf(ids);
        return (b, s, l, c) -> set.contains(b.getId());
    }

    /**
     * A {@link LimitMatcher} which matches the contingency with the specified ID.
     */
    static LimitMatcher contingency(String id) {
        return (b, s, l, c) -> c != null && c.getId().equals(id);
    }

    /**
     * A {@link LimitMatcher} which matches the contingencies with the specified ID.
     */
    static LimitMatcher contingencies(Collection<String> ids) {
        Set<String> set = ImmutableSet.copyOf(ids);
        return (b, s, l, c) -> c != null && set.contains(c.getId());
    }

}
