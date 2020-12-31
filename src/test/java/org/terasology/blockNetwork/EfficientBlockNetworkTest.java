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
import org.terasology.math.SideBitFlag;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class EfficientBlockNetworkTest {
    private EfficientBlockNetwork<NetworkNode> blockNetwork;
    private TestListenerEfficient listener;
    private byte allDirections;

    @Before
    public void setup() {
        blockNetwork = new EfficientBlockNetwork<>();
        listener = new TestListenerEfficient();
        blockNetwork.addTopologyListener(listener);
        blockNetwork.addTopologyListener(new ValidatingListenerEfficient());
        allDirections = 63;
    }

    private NetworkNode toNode(Vector3ic location, byte directions) {
        return new NetworkNode(location, directions);
    }

    @Test
    public void twoLeafNetworkCreationAndDestruction() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());
        listener.validateNumbers(0, 0, 0, 0, 0, 0);
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 0, 0, 0, 0);

        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 1, 0, 0, 0, 0);
        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 1, 0, 0, 0, 0);
    }

    @Test
    public void addAndRemoveNetworkingBlock() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 0, 0, 0, 0);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 1, 0, 0, 0, 0);
    }

    @Test
    public void addAndRemoveLeafBlock() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());

        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(0, blockNetwork.getNetworks().size());

        listener.validateNumbers(0, 0, 0, 0, 0, 0);
    }

    @Test
    public void addTwoNeighbouringLeafBlocks() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        listener.validateNumbers(0, 0, 0, 0, 0, 0);

        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        Network2<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 1), allDirections)));

        listener.validateNumbers(1, 0, 0, 0, 0, 0);
    }

    @Test
    public void addLeafNodeThenNetworkingNode() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        listener.validateNumbers(0, 0, 0, 0, 0, 0);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        Network2<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));

        listener.validateNumbers(1, 0, 0, 0, 0, 0);
    }

    @Test
    public void addTwoNeighbouringNetworkingBlocks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 0, 0, 0, 0);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 1, 0, 0, 0);
    }

    @Test
    public void newNetworkingNodeJoinsLeafNodeIntoExistingNetwork() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addLeafBlock(toNode(new Vector3i(1, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        Network2<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertFalse(network.hasLeafNode(toNode(new Vector3i(1, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        listener.validateNumbers(1, 0, 0, 0, 0, 0);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasLeafNode(toNode(new Vector3i(1, 0, 0), allDirections)));
        listener.validateNumbers(1, 0, 1, 0, 1, 0);
    }

    @Test
    public void removingNetworkingNodeSplitsNetworkInTwo() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 2, 0, 0, 0);

        blockNetwork.removeNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(2, blockNetwork.getNetworks().size());
        listener.validateNumbers(3, 1, 2, 0, 0, 0);
    }

    @Test
    public void addingNetworkingNodeJoinsExistingNetworks() {
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, -1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(2, blockNetwork.getNetworks().size());
        listener.validateNumbers(2, 0, 0, 0, 0, 0);

        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        assertEquals(1, blockNetwork.getNetworks().size());
        Network<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, -1), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        listener.validateNumbers(3, 2, 0, 0, 0, 0);
    }

    @Test
    public void addLeafNetworkingLeaf() {
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 2), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(toNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);

        blockNetwork.removeLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addLeafBlock(toNode(new Vector3i(0, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);

        assertEquals(1, blockNetwork.getNetworks().size());

        Network<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 0), allDirections)));
        assertTrue(network.hasNetworkingNode(toNode(new Vector3i(0, 0, 1), allDirections)));
        assertTrue(network.hasLeafNode(toNode(new Vector3i(0, 0, 2), allDirections)));
    }

    @Test
    public void addTwoOverlappingCrossingNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.RIGHT, Side.LEFT), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.FRONT, Side.BACK), NetworkChangeReason.WORLD_CHANGE);

        assertEquals(2, blockNetwork.getNetworks().size());
    }

    @Test
    public void tryAddingOverlappingConnectionsNetworkingNodes() {
        Vector3i location = new Vector3i(0, 0, 0);
        blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.RIGHT, Side.LEFT), NetworkChangeReason.WORLD_CHANGE);
        try {
            blockNetwork.addNetworkingBlock(new NetworkNode(location, Side.FRONT, Side.BACK, Side.RIGHT), NetworkChangeReason.WORLD_CHANGE);
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException exp) {
            // expected
        }
    }

    @Test
    public void networkingNodesInTheSameBlockCanConnectAndHaveCorrectDistance() {
        Vector3i location = new Vector3i(0, 0, 0);
        final NetworkNode leftRight = new NetworkNode(location, Side.RIGHT, Side.LEFT);
        blockNetwork.addNetworkingBlock(leftRight, NetworkChangeReason.WORLD_CHANGE);
        final NetworkNode frontBack = new NetworkNode(location, Side.FRONT, Side.BACK);
        blockNetwork.addNetworkingBlock(frontBack, NetworkChangeReason.WORLD_CHANGE);

        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(1, 0, 1), allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(1, 0, 0), allDirections), NetworkChangeReason.WORLD_CHANGE);

        assertEquals(1, blockNetwork.getNetworks().size());

        Network<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertEquals(4, network.getDistance(leftRight, frontBack, Integer.MAX_VALUE));
    }

    @Test
    public void networkingNodesNotConnectingIfInputOutputConflict() {
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 0), allDirections, (byte) 0), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 1), allDirections, (byte) 0), NetworkChangeReason.WORLD_CHANGE);

        assertEquals(2, blockNetwork.getNetworks().size());
        listener.validateNumbers(2, 0, 0, 0, 0, 0);
    }

    @Test
    public void networkingNodesConnectingWithCorrectInputOutput() {
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 0), allDirections, (byte) 0), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 1), (byte) 0, allDirections), NetworkChangeReason.WORLD_CHANGE);

        assertEquals(1, blockNetwork.getNetworks().size());
        listener.validateNumbers(1, 0, 1, 0, 0, 0);
    }

    @Test
    public void networkingNodesDistanceWithInputOutput() {
        NetworkNode startBlock = new NetworkNode(new Vector3i(0, 0, 0), (byte) 0, allDirections);
        NetworkNode endBlock = new NetworkNode(new Vector3i(0, 1, 0), SideBitFlag.getSide(Side.FRONT), SideBitFlag.getSides(Side.BACK, Side.LEFT, Side.RIGHT, Side.TOP, Side.BOTTOM));

        blockNetwork.addNetworkingBlock(startBlock, NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 0, 1), allDirections, allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(new NetworkNode(new Vector3i(0, 1, 1), allDirections, allDirections), NetworkChangeReason.WORLD_CHANGE);
        blockNetwork.addNetworkingBlock(endBlock, NetworkChangeReason.WORLD_CHANGE);

        assertEquals(1, blockNetwork.getNetworks().size());
        Network<NetworkNode> network = blockNetwork.getNetworks().iterator().next();
        assertEquals(3, network.getDistance(startBlock, endBlock, Integer.MAX_VALUE));
    }

    private class TestListenerEfficient implements EfficientNetworkTopologyListener<NetworkNode> {
        private int networksAdded;
        private int networksRemoved;
        private int networkingNodesAdded;
        private int networkingNodesRemoved;
        private int leafNodesAdded;
        private int leafNodesRemoved;

        public void reset() {
            networksAdded = 0;
            networksRemoved = 0;
            networkingNodesAdded = 0;
            networkingNodesRemoved = 0;
            leafNodesAdded = 0;
            leafNodesRemoved = 0;
        }

        @Override
        public void networkAdded(Network2 newNetwork, NetworkChangeReason reason) {
            networksAdded++;
        }

        @Override
        public void networkRemoved(Network2 network, NetworkChangeReason reason) {
            networksRemoved++;
        }

        @Override
        public void networkSplit(Network2<NetworkNode> oldNetwork, Set<? extends Network2<NetworkNode>> resultNetwork, NetworkChangeReason reason) {
            networksRemoved++;
            networksAdded += resultNetwork.size();
        }

        @Override
        public void networksMerged(Set<? extends Network2<NetworkNode>> oldNetworks, Network2<NetworkNode> newNetwork, NetworkChangeReason reason) {
            networksRemoved += oldNetworks.size();
            networksAdded++;
        }

        @Override
        public void networkingNodesAdded(Network2<NetworkNode> network, Set<NetworkNode> networkingNodes, NetworkChangeReason reason) {
            networkingNodesAdded++;
        }

        @Override
        public void networkingNodesRemoved(Network2<NetworkNode> network, Set<NetworkNode> networkingNodes, NetworkChangeReason reason) {
            networkingNodesRemoved++;
        }

        @Override
        public void leafNodesAdded(Network2<NetworkNode> network, Set<NetworkNode> leafNodes, NetworkChangeReason reason) {
            leafNodesAdded++;
        }

        @Override
        public void leafNodesRemoved(Network2<NetworkNode> network, Set<NetworkNode> leafNodes, NetworkChangeReason reason) {
            leafNodesRemoved++;
        }

        public void validateNumbers(int networksAddedExpected, int networksRemovedExpected, int networkingNodesAddedExpected, int networkingNodesRemovedExpected,
                                    int leafNodesAddedExpected, int leafNodesRemovedExpected) {
            assertEquals("Networks added", networksAddedExpected, networksAdded);
            assertEquals("Networks removed", networksRemovedExpected, networksRemoved);
            assertEquals("Networking nodes added", networkingNodesAddedExpected, networkingNodesAdded);
            assertEquals("Networking nodes removed", networkingNodesRemovedExpected, networkingNodesRemoved);
            assertEquals("Leaf nodes added", leafNodesAddedExpected, leafNodesAdded);
            assertEquals("Leaf nodes removed", leafNodesRemovedExpected, leafNodesRemoved);
        }
    }

    private class ValidatingListenerEfficient implements EfficientNetworkTopologyListener<NetworkNode> {
        private Set<Network> networks = Sets.newHashSet();
        private Multimap<Network, NetworkNode> networkingNodes = HashMultimap.create();
        private Multimap<Network, NetworkNode> leafNodes = HashMultimap.create();

        @Override
        public void networkSplit(Network2<NetworkNode> oldNetwork, Set<? extends Network2<NetworkNode>> resultNetwork, NetworkChangeReason reason) {

        }

        @Override
        public void networksMerged(Set<? extends Network2<NetworkNode>> oldNetworks, Network2<NetworkNode> newNetwork, NetworkChangeReason reason) {

        }

        @Override
        public void networkAdded(Network2<NetworkNode> network, NetworkChangeReason reason) {
            assertTrue(networks.add(network));
            for (NetworkNode networkNode : network.getNetworkingNodes()) {
                assertTrue(this.networkingNodes.put(network, networkNode));
            }
            for (NetworkNode leafNode : network.getLeafNodes()) {
                assertTrue(this.leafNodes.put(network, leafNode));
            }
        }

        @Override
        public void networkingNodesAdded(Network2<NetworkNode> network, Set<NetworkNode> networkingNodesAdded, NetworkChangeReason reason) {
            assertTrue(networks.contains(network));
            for (NetworkNode networkingNode : networkingNodesAdded) {
                assertTrue(this.networkingNodes.put(network, networkingNode));
            }
        }

        @Override
        public void networkingNodesRemoved(Network2<NetworkNode> network, Set<NetworkNode> networkingNodesRemoved, NetworkChangeReason reason) {
            assertTrue(networks.contains(network));
            for (NetworkNode networkingNode : networkingNodesRemoved) {
                assertTrue(this.networkingNodes.remove(network, networkingNode));
            }
        }

        @Override
        public void leafNodesAdded(Network2<NetworkNode> network, Set<NetworkNode> leafNodesAdded, NetworkChangeReason reason) {
            assertTrue(networks.contains(network));
            for (NetworkNode leafNode : leafNodesAdded) {
                assertTrue(this.leafNodes.put(network, leafNode));
            }
        }

        @Override
        public void leafNodesRemoved(Network2<NetworkNode> network, Set<NetworkNode> leafNodesRemoved, NetworkChangeReason reason) {
            assertTrue(networks.contains(network));
            for (NetworkNode leafNode : leafNodesRemoved) {
                assertTrue(this.leafNodes.remove(network, leafNode));
            }
        }

        @Override
        public void networkRemoved(Network2<NetworkNode> network, NetworkChangeReason reason) {
            for (NetworkNode networkNode : network.getNetworkingNodes()) {
                assertTrue(this.networkingNodes.remove(network, networkNode));
            }
            for (NetworkNode leafNode : network.getLeafNodes()) {
                assertTrue(this.leafNodes.remove(network, leafNode));
            }

            assertFalse(networkingNodes.containsKey(network));
            assertFalse(leafNodes.containsKey(network));
            assertTrue(networks.remove(network));
        }
    }
}
