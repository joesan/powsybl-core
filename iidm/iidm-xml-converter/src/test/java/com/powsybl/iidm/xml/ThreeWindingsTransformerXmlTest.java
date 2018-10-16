/**
 * Copyright (c) 2016, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.powsybl.iidm.xml;

import java.io.IOException;
import java.io.InputStream;

import com.powsybl.iidm.network.Network;
import com.powsybl.iidm.network.RatioTapChanger;
import com.powsybl.iidm.network.ThreeWindingsTransformer;
import org.junit.Test;

import com.powsybl.commons.AbstractConverterTest;
import com.powsybl.iidm.network.test.ThreeWindingsTransformerNetworkFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Luma Zamarreno <zamarrenolm at aia.es>
 * @author Yichen Tang <yichen.tang at rte-france.com>
 */
public class ThreeWindingsTransformerXmlTest extends AbstractConverterTest {

    @Test
    public void roundTripTest() throws IOException {
        roundTripXmlTest(ThreeWindingsTransformerNetworkFactory.createWithCurrentLimits(),
                         NetworkXml::writeAndValidate,
                         NetworkXml::read,
                         "/threeWindingsTransformerRoundTripRef.xml");
    }

    @Test
    public void testReadV10() {
        InputStream in = getClass().getResourceAsStream("/refs_V1_0/threeWindingsTransformerRoundTripRef.xml");
        Network read = NetworkXml.read(in);
        ThreeWindingsTransformer.Leg2or3 leg2 = read.getThreeWindingsTransformer("3WT").getLeg2();
        ThreeWindingsTransformer.Leg2or3 leg3 = read.getThreeWindingsTransformer("3WT").getLeg3();
        assertEquals(0.9801, leg2.getRatioTapChanger().getTap(0).getRdr(), 0.0);
        assertEquals(0.1089, leg2.getRatioTapChanger().getTap(1).getRdx(), 0.0);
        assertEquals(0.8264462809917356, leg3.getRatioTapChanger().getTap(1).getRdg(), 0.0);
        assertEquals(0.09090909090909093, leg3.getRatioTapChanger().getTap(2).getRdb(), 0.0);
        RatioTapChanger tapChanger = leg2.getRatioTapChanger();
        assertEquals(0.9, tapChanger.getTap(0).getRatio(), 0.0);
        assertEquals(1.0, tapChanger.getTap(1).getRatio(), 0.0);
        assertEquals(1.1, tapChanger.getTap(2).getRatio(), 0.0);
        assertTrue(leg2.getRatioTapChanger().onLoadTapChanger());
    }
}
