/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.security.dsl;

import com.powsybl.contingency.Contingency;
import com.powsybl.iidm.network.*;
import com.powsybl.iidm.network.test.EurostagTutorialExample1Factory;
import com.powsybl.security.LimitViolation;
import com.powsybl.security.LimitViolationDetector;
import com.powsybl.security.SecurityAnalysisInputs;
import groovy.lang.GroovyCodeSource;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * @author Sylvain Leclerc <sylvain.leclerc at rte-france.com>
 */
public class LimitFactorsTest {

    private Network network;
    private Line line;
    private TwoWindingsTransformer transformer;
    private CurrentLimits.TemporaryLimit temporaryLimit;

    @Before
    public void setUp() {
        network = EurostagTutorialExample1Factory.create();

        line = network.getLine("NHV1_NHV2_1");
        transformer = network.getTwoWindingsTransformer("NGEN_NHV1");

        line.newCurrentLimits1()
                .setPermanentLimit(1000)
                .beginTemporaryLimit()
                .setName("IT20")
                .setAcceptableDuration(1200)
                .setValue(1500)
                .endTemporaryLimit()
                .beginTemporaryLimit()
                .setName("IT10")
                .setAcceptableDuration(600)
                .setValue(2000)
                .endTemporaryLimit()
                .add();

        transformer.newCurrentLimits1()
                .setPermanentLimit(1000)
                .beginTemporaryLimit()
                .setName("IT20")
                .setAcceptableDuration(1200)
                .setValue(1500)
                .endTemporaryLimit()
                .beginTemporaryLimit()
                .setName("IT10")
                .setAcceptableDuration(600)
                .setValue(2000)
                .endTemporaryLimit()
                .add();

        temporaryLimit = line.getCurrentLimits1().getTemporaryLimit(1200);
    }

    @Test
    public void test() {

        LimitFactors factors = new LimitFactorsLoader(new GroovyCodeSource(getClass().getResource("/limits-dsl.groovy")))
                .loadFactors();

        assertEquals(0.95f, factors.getFactor(line, Branch.Side.ONE, null, null).orElse(1.0f), 0);
        assertEquals(1.0f, factors.getFactor(line, Branch.Side.ONE, temporaryLimit, null).orElse(1.0f), 0);
        assertEquals(0.99f, factors.getFactor(line, Branch.Side.TWO, null, new Contingency("contingency1")).orElse(1.0f), 0);
        assertEquals(0.98f, factors.getFactor(line, Branch.Side.TWO, null, new Contingency("toto")).orElse(1.0f), 0);
    }


    private void checkDetector(LimitViolationDetector detector) {

        assertThat(detector).isInstanceOf(LimitViolationDetectorWithFactors.class);

        List<LimitViolation> violations = new ArrayList<>();
        detector.checkCurrent(null, line, Branch.Side.ONE, 960, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(960f, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, line, Branch.Side.ONE, 940, violations::add);
        assertThat(violations).isEmpty();

        violations.clear();
        detector.checkCurrent(null, line, Branch.Side.TWO, 2000, violations::add);
        assertThat(violations).isEmpty();

        violations.clear();
        detector.checkCurrent(null, line, Branch.Side.ONE, 1499, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(1499, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, line, Branch.Side.ONE, 1501, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(1200, v.getAcceptableDuration());
                    assertEquals(1501f, v.getValue(), 0f);
                    assertEquals(1500f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(new Contingency("toto"), line, Branch.Side.ONE, 979, violations::add);
        assertThat(violations).isEmpty();

        violations.clear();
        detector.checkCurrent(new Contingency("toto"), line, Branch.Side.ONE, 981, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(981, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(new Contingency("contingency1"), line, Branch.Side.ONE, 989, violations::add);
        assertThat(violations).isEmpty();

        violations.clear();
        detector.checkCurrent(new Contingency("contingency1"), line, Branch.Side.ONE, 991, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(991, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });
    }

    @Test
    public void testDetector() {

        LimitViolationDetector detector = new LimitFactorsLoader(new GroovyCodeSource(getClass().getResource("/limits-dsl.groovy")))
                .loadDetector();

        checkDetector(detector);
    }

    @Test
    public void testConfigurer() {
        SecurityAnalysisInputs inputs = new SecurityAnalysisInputs();
        new SecurityAnalysisDslConfigurerFactory().create(getClass().getResourceAsStream("/limits-dsl.groovy"))
            .configure(network, inputs);

        checkDetector(inputs.getLimitViolationDetector());
    }

    @Test
    public void testExpressions() {
        SecurityAnalysisInputs inputs = new SecurityAnalysisInputs();
        new SecurityAnalysisDslConfigurerFactory().create(getClass().getResourceAsStream("/limits-dsl-expressions.groovy"))
                .configure(network, inputs);

        LimitViolationDetector detector = inputs.getLimitViolationDetector();

        List<LimitViolation> violations = new ArrayList<>();

        //Check 0.9 factor
        detector.checkCurrent(null, line, Branch.Side.ONE, 910, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(910, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, line, Branch.Side.ONE, 890, violations::add);
        assertThat(violations).isEmpty();


        //Check 0.8 factor
        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 810, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                    assertEquals(810, v.getValue(), 0f);
                    assertEquals(1000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 790, violations::add);
        assertThat(violations).isEmpty();

        //Check 0.7 factor
        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 0.7 * 1500 + 10, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(1200, v.getAcceptableDuration());
                    assertEquals(0.7 * 1500 + 10, v.getValue(), 0f);
                    assertEquals(1500, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 0.7 * 1500 - 10, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(Integer.MAX_VALUE, v.getAcceptableDuration());
                });
        //Check 0.6 factor
        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 0.6 * 2000 + 10, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(600, v.getAcceptableDuration());
                    assertEquals(0.6 * 2000 + 10, v.getValue(), 0f);
                    assertEquals(2000f, v.getLimit(), 0f);
                });

        violations.clear();
        detector.checkCurrent(null, transformer, Branch.Side.ONE, 0.6 * 2000 - 10, violations::add);
        assertThat(violations)
                .hasOnlyOneElementSatisfying(v -> {
                    assertEquals(1200, v.getAcceptableDuration());
                });
    }
}
