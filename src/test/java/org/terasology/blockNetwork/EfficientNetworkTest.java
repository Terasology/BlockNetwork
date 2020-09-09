// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import org.junit.Before;
import org.junit.Test;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EfficientNetworkTest {
    private EfficientNetwork<NetworkNode> network;
    private byte allDirections;
    private byte upOnly;

    @Before
    public void setup() {
        network = new EfficientNetwork<>();
        allDirections = 63;
        upOnly = SideBitFlag.addSide((byte) 0, Side.TOP);
    }

    private NetworkNode toNode(Vector3i location, byte sides) {
        return new NetworkNode(location, sides);
    }

    private NetworkNode toNode(Vector3i location, byte inputSides, byte outputSides) {
        return new NetworkNode(location, inputSides, outputSides);
    }

    @Test
    public void addNetworkingNodeToEmptyNetwork() {
        assertTrue(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    @Test
    public void cantAddLeafNodeToEmptyNetwork() {
        assertFalse(network.canAddLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void addingLeafNodeToNetworkingNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));

        assertTrue(network.canAddLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        network.addLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworkSize());
    }

    @Test
    public void creatingDegenerateNetwork() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 1), allDirections),
                toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworkSize());
    }

    @Test
    public void cantAddNetworkingNodeToDegeneratedNetwork() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 1), allDirections),
                toNode(new Vector3i(0, 0, 0), allDirections));
        assertFalse(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 2), allDirections)));
    }

    @Test
    public void cantAddLeafNodeToDegeneratedNetwork() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 1), allDirections),
                toNode(new Vector3i(0, 0, 0), allDirections));
        assertFalse(network.canAddLeafNode(toNode(new Vector3i(0, 0, 2), allDirections)));
    }

    @Test
    public void addingNetworkingNodeToNetworkingNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));

        assertTrue(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, network.getNetworkSize());
    }

    @Test
    public void cantAddNodeToNetworkingNodeTooFar() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 2), allDirections));

        assertFalse(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void cantAddNodeToNetworkingNodeWrongDirection() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), upOnly));

        assertFalse(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void cantAddNodeToNetworkOnTheSideOfConnectedLeaf() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 2), allDirections));
        network.addLeafNode(toNode(new Vector3i(0, 0, 1), allDirections));

        assertFalse(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void canAddLeafNodeOnTheSideOfConnectedNetworkingNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addLeafNode(toNode(new Vector3i(0, 0, 2), allDirections));

        assertTrue(network.canAddLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        network.addLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));
    }

    @Test
    public void canAddNetworkingNodeOnTheSideOfConnectedNetworkingNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addLeafNode(toNode(new Vector3i(0, 0, 2), allDirections));

        assertTrue(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(3, network.getNetworkSize());
    }

    @Test
    public void cantAddNodeToNetworkWithTwoLeafNodes() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 2), allDirections),
                toNode(new Vector3i(0, 0, 1), allDirections));

        assertFalse(network.canAddNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
    }

    @Test
    public void removeLeafNodeFromConnectedNetworkWithNetworkingNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));

        network.removeLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    @Test
    public void removeLeafNodeFromConnectedNetworkWithOnlyLeafNodes() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 0), allDirections),
                toNode(new Vector3i(0, 0, 1), allDirections));

        assertTrue(network.isTwoLeafNetwork());
        network.removeLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, network.getNetworkSize());
    }

    @Test
    public void distanceForSameLeafNode() {
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addLeafNode(toNode(new Vector3i(0, 0, 0), allDirections));

        assertEquals(0, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                0), allDirections), 0));
        assertEquals(0, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                0), allDirections), Integer.MAX_VALUE));
    }

    @Test
    public void distanceForDegeneratedNetwork() {
        network = EfficientNetwork.createLeafOnlyNetwork(toNode(new Vector3i(0, 0, 0), allDirections),
                toNode(new Vector3i(0, 0, 1), allDirections));

        assertEquals(1, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                1), allDirections), 1));
        assertEquals(1, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                1), allDirections), Integer.MAX_VALUE));
    }

    @Test
    public void distanceForTwoLeafNodesOnNetwork() {
        NetworkNode firstLeaf = toNode(new Vector3i(0, 0, 0), allDirections);
        NetworkNode secondLeaf = toNode(new Vector3i(0, 0, 2), allDirections);
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addLeafNode(secondLeaf);
        network.addLeafNode(firstLeaf);

        assertEquals(2, network.getDistance(firstLeaf, secondLeaf, 2));
        assertEquals(-1, network.getDistance(firstLeaf, secondLeaf, 1));
        assertEquals(2, network.getDistance(firstLeaf, secondLeaf, Integer.MAX_VALUE));
    }

    @Test
    public void distanceFromDifferentSides() {
        NetworkNode firstLeaf = toNode(new Vector3i(0, 0, 0), allDirections);
        NetworkNode secondLeaf = toNode(new Vector3i(0, 0, 2), allDirections);
        network.addNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections));
        network.addNetworkingNode(toNode(new Vector3i(0, 1, 1), allDirections));
        network.addNetworkingNode(toNode(new Vector3i(0, 1, 2), allDirections));
        network.addLeafNode(secondLeaf);
        network.addLeafNode(firstLeaf);

        assertEquals(2, network.getDistanceWithSide(firstLeaf, secondLeaf, Side.FRONT, 2));
        assertEquals(-1, network.getDistanceWithSide(firstLeaf, secondLeaf, Side.TOP, 2));
        assertEquals(-1, network.getDistanceWithSide(firstLeaf, secondLeaf, Side.TOP, 3));
        assertEquals(4, network.getDistanceWithSide(firstLeaf, secondLeaf, Side.TOP, 4));
    }

    @Test
    public void distanceForLongNetwork() {
        for (int i = 0; i < 10; i++) {
            network.addNetworkingNode(toNode(new Vector3i(0, 0, i), allDirections));
        }

        assertEquals(9, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                9), allDirections), 9));
        assertEquals(-1, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                9), allDirections), 8));
        assertEquals(9, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(0, 0,
                9), allDirections), Integer.MAX_VALUE));
    }

    @Test
    public void distanceForBranchedNetwork() {
        for (int i = 0; i < 10; i++) {
            network.addNetworkingNode(toNode(new Vector3i(0, 0, i), allDirections));
        }

        for (int i = 1; i <= 5; i++) {
            network.addNetworkingNode(toNode(new Vector3i(i, 0, 5), allDirections));
        }

        assertEquals(10, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0,
                5), allDirections), 10));
        assertEquals(-1, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0,
                5), allDirections), 9));
        assertEquals(10, network.getDistance(toNode(new Vector3i(0, 0, 0), allDirections), toNode(new Vector3i(5, 0,
                5), allDirections), Integer.MAX_VALUE));
    }

    @Test
    public void shortestRouteForBranchedNetwork() {
        for (int i = 0; i < 10; i++) {
            network.addNetworkingNode(toNode(new Vector3i(0, 0, i), allDirections));
        }

        for (int i = 1; i <= 5; i++) {
            network.addNetworkingNode(toNode(new Vector3i(i, 0, 5), allDirections));
        }

        final List<NetworkNode> route = network.findShortestRoute(toNode(new Vector3i(0, 0, 0), allDirections),
                toNode(new Vector3i(5, 0, 5), allDirections));
        assertEquals(11, route.size());
        assertEquals(toNode(new Vector3i(0, 0, 0), allDirections), route.get(0));
        assertEquals(toNode(new Vector3i(0, 0, 1), allDirections), route.get(1));
        assertEquals(toNode(new Vector3i(0, 0, 2), allDirections), route.get(2));
        assertEquals(toNode(new Vector3i(0, 0, 3), allDirections), route.get(3));
        assertEquals(toNode(new Vector3i(0, 0, 4), allDirections), route.get(4));
        assertEquals(toNode(new Vector3i(0, 0, 5), allDirections), route.get(5));
        assertEquals(toNode(new Vector3i(1, 0, 5), allDirections), route.get(6));
        assertEquals(toNode(new Vector3i(2, 0, 5), allDirections), route.get(7));
        assertEquals(toNode(new Vector3i(3, 0, 5), allDirections), route.get(8));
        assertEquals(toNode(new Vector3i(4, 0, 5), allDirections), route.get(9));
        assertEquals(toNode(new Vector3i(5, 0, 5), allDirections), route.get(10));
    }
}
