/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.blockNetwork;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.junit.Before;
import org.junit.Test;
import org.terasology.math.Side;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class BlockNetworkTest {
    private BlockNetwork<NetworkNode> blockNetwork;
    private TestListener listener;
    private byte allDirections;

    @Before
    public void setup() {
        blockNetwork = new BlockNetwork();
        listener = new TestListener();
        blockNetwork.addTopologyListener(listener);
        blockNetwork.addTopologyListener(new ValidatingListener());
        allDirections = 63;
    }

    private NetworkNode toNode(Vector3ic location, byte directions) {
        return new NetworkNode(location, directions);
    }

    @Test
    public void addAndRemoveNetworkingBlock() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(0, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksRemoved);
    }

    @Test
    public void addAndRemoveLeafBlock() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(0, blockNetwork.getNetworks().size());

        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(0, blockNetwork.getNetworks().size());

        assertEquals(0, listener.networksAdded);
        assertEquals(0, listener.networksRemoved);
    }

    @Test
    public void addTwoNeighbouringLeafBlocks() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(0, listener.networksAdded);

        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);
    }

    @Test
    public void addLeafNodeThenNetworkingNode() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        assertEquals(0, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        Network network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));

        assertEquals(1, listener.networksAdded);
    }

    @Test
    public void addTwoNeighbouringNetworkingBlocks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());

        assertEquals(1, listener.networksAdded);
        assertEquals(2, listener.networkingNodesAdded);
    }

    @Test
    public void newNetworkingNodeJoinsLeafNodeIntoExistingNetwork() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addLeafBlock(toNode(new Vector3i(1, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        Network network = blockNetwork.getNetworks().iterator().next();
        assertFalse(network.hasLeafNode(toNode(new Vector3i(1, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertEquals(1, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasLeafNode(toNode(new Vector3i(1, 0, 0), allDirections)));
        assertEquals(1, listener.networksAdded);
        assertEquals(2, listener.networkingNodesAdded);
        assertEquals(1, listener.leafNodesAdded);
    }

    @Test
    public void removingNetworkingNodeSplitsNetworkInTwo() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        assertEquals(1, listener.networksAdded);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void addingNetworkingNodeJoinsExistingNetworks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections));
        assertEquals(2, blockNetwork.getNetworks().size());
        assertEquals(2, listener.networksAdded);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        assertEquals(1, blockNetwork.getNetworks().size());
        Network network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, -1), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertEquals(1, listener.networksRemoved);
    }

    @Test
    public void addLeafNetworkingLeaf() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 2), allDirections));
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));

        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections));

        assertEquals(1, blockNetwork.getNetworks().size());

        Network network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 2), allDirections)));
    }

    @Test
    public void addTwoOverlappingCrossingNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.RIGHT, Side.LEFT));
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.FRONT, Side.BACK));

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void tryAddingOverlappingConnectionsNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.RIGHT, Side.LEFT));
        try {
            blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.FRONT, Side.BACK, Side.RIGHT));
            fail("Expected IllegalStateException");
        } catch (IllegalStateException exp) {
            // expected
        }
    }

    @Test
    public void cablesInTheSameBlockCanConnectAndHaveCorrectDistance() {
        Vector3i location = new Vector3i(0, 0, 0);
        final NetworkNode leftRight = new NetworkNode(location, Side.RIGHT, Side.LEFT);
        blockNetwork.addNetworkingBlock(leftRight);
        final NetworkNode frontBack = new NetworkNode(location, Side.FRONT, Side.BACK);
        blockNetwork.addNetworkingBlock(frontBack);

        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(1, 0, 1), allDirections));
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(1, 0, 0), allDirections));

        assertEquals(1, blockNetwork.getNetworks().size());

        Network network = blockNetwork.getNetworks().iterator().next();
        assertEquals(4, network.getDistance(leftRight, frontBack, Integer.MAX_VALUE));
    }

    private class TestListener implements NetworkTopologyListener<NetworkNode> {
        public int networksAdded;
        public int networksRemoved;
        public int networkingNodesAdded;
        public int networkingNodesRemoved;
        public int leafNodesAdded;
        public int leafNodesRemoved;

        public void reset() {
            networksAdded = 0;
            networksRemoved = 0;
            networkingNodesAdded = 0;
            networkingNodesRemoved = 0;
            leafNodesAdded = 0;
            leafNodesRemoved = 0;
        }

        @Override
        public void networkAdded(Network newNetwork) {
            networksAdded++;
        }

        @Override
        public void networkRemoved(Network network) {
            networksRemoved++;
        }

        @Override
        public void networkingNodesAdded(Network<NetworkNode> network, Set<NetworkNode> networkingNodes) {
            networkingNodesAdded++;
        }

        @Override
        public void networkingNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> networkingNodes) {
            networkingNodesRemoved++;
        }

        @Override
        public void leafNodesAdded(Network<NetworkNode> network, Set<NetworkNode> leafNodes) {
            leafNodesAdded++;
        }

        @Override
        public void leafNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> leafNodes) {
            leafNodesRemoved++;
        }
    }

    private class ValidatingListener implements NetworkTopologyListener<NetworkNode> {
        private Set<Network> networks = Sets.newHashSet();
        private Multimap<Network, NetworkNode> networkingNodes = HashMultimap.create();
        private Multimap<Network, NetworkNode> leafNodes = HashMultimap.create();

        @Override
        public void networkAdded(Network network) {
            assertTrue(networks.add(network));
        }

        @Override
        public void networkingNodesAdded(Network<NetworkNode> network, Set<NetworkNode> networkingNodesAdded) {
            assertTrue(networks.contains(network));
            for (NetworkNode networkingNode : networkingNodesAdded) {
                assertTrue(this.networkingNodes.put(network, networkingNode));
            }
        }

        @Override
        public void networkingNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> networkingNodesRemoved) {
            assertTrue(networks.contains(network));
            for (NetworkNode networkingNode : networkingNodesRemoved) {
                assertTrue(this.networkingNodes.remove(network, networkingNode));
            }
        }

        @Override
        public void leafNodesAdded(Network<NetworkNode> network, Set<NetworkNode> leafNodesAdded) {
            assertTrue(networks.contains(network));
            for (NetworkNode leafNode : leafNodesAdded) {
                assertTrue(this.leafNodes.put(network, leafNode));
            }
        }

        @Override
        public void leafNodesRemoved(Network<NetworkNode> network, Set<NetworkNode> leafNodesRemoved) {
            assertTrue(networks.contains(network));
            for (NetworkNode leafNode : leafNodesRemoved) {
                assertTrue(this.leafNodes.remove(network, leafNode));
            }
        }

        @Override
        public void networkRemoved(Network network) {
            assertFalse(networkingNodes.containsKey(network));
            assertFalse(leafNodes.containsKey(network));
            assertTrue(networks.remove(network));
        }
    }
}
