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
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @author Marcin Sciesinski <marcins78@gmail.com>
 */
public class BlockNetwork {
    private Set<SimpleNetwork> networks = Sets.newHashSet();
    private Multimap<ImmutableBlockLocation, NetworkNode> leafNodes = HashMultimap.create();
    private Multimap<ImmutableBlockLocation, NetworkNode> networkingNodes = HashMultimap.create();

    private Set<NetworkTopologyListener> listeners = new HashSet<>();

    private boolean mutating;

    public void addTopologyListener(NetworkTopologyListener listener) {
        listeners.add(listener);
    }

    public void removeTopologyListener(NetworkTopologyListener listener) {
        listeners.remove(listener);
    }

    private void validateNotMutating() {
        if (mutating) {
            throw new IllegalStateException("Can't modify block network while modification is in progress");
        }
    }

    public void addNetworkingBlock(NetworkNode networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            validateNoNetworkingOverlap(networkNode);
            networkingNodes.put(networkNode.location, networkNode);

            addNetworkingBlockInternal(networkNode);
        } finally {
            mutating = false;
        }
    }

    public void addNetworkingBlocks(Collection<NetworkNode> networkNodes) {
        // No major optimization possible here
        for (NetworkNode networkNode : networkNodes) {
            addNetworkingBlock(networkNode);
        }
    }

    private void validateNoNetworkingOverlap(NetworkNode networkNode) {
        for (NetworkNode nodeAtSamePosition : networkingNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalStateException("There is a networking block at that position connecting to some of the same sides already");
            }
        }
    }

    private void validateNoLeafOverlap(NetworkNode networkNode) {
        for (NetworkNode nodeAtSamePosition : leafNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalStateException("There is a leaf block at that position connecting to some of the same sides already");
            }
        }
    }

    private void addNetworkingBlockInternal(NetworkNode networkNode) {
        SimpleNetwork addToNetwork = null;

        Set<NetworkNode> networkingNodesToAdd = Sets.newHashSet();
        networkingNodesToAdd.add(networkNode);

        Set<NetworkNode> newLeafNodes = Sets.newHashSet();

        // Try adding to existing networks
        final Iterator<SimpleNetwork> networkIterator = networks.iterator();
        while (networkIterator.hasNext()) {
            final SimpleNetwork network = networkIterator.next();
            if (network.canAddNetworkingNode(networkNode)) {
                if (addToNetwork == null) {
                    addToNetwork = network;
                } else {
                    Set<NetworkNode> networkingNodesToNotify = Sets.newHashSet(network.getNetworkingNodes());
                    Set<NetworkNode> leafNodesToNotify = Sets.newHashSet(network.getLeafNodes());

                    networkingNodesToAdd.addAll(networkingNodesToNotify);
                    newLeafNodes.addAll(leafNodesToNotify);

                    network.removeAllLeafNodes();
                    notifyLeafNodesRemoved(network, leafNodesToNotify);
                    network.removeAllNetworkingNodes();
                    notifyNetworkingNodesRemoved(network, networkingNodesToNotify);

                    networkIterator.remove();
                    notifyNetworkRemoved(network);
                }
            }
        }

        // If it's not in any networks, create a new one
        if (addToNetwork == null) {
            SimpleNetwork newNetwork = new SimpleNetwork();
            networks.add(newNetwork);
            notifyNetworkAdded(newNetwork);
            addToNetwork = newNetwork;
        }

        for (NetworkNode networkingNode : networkingNodesToAdd) {
            addToNetwork.addNetworkingNode(networkingNode);
        }
        notifyNetworkingNodesAdded(addToNetwork, networkingNodesToAdd);

        for (NetworkNode leafNode : newLeafNodes) {
            addToNetwork.addLeafNode(leafNode);
        }

        // Find all leaf nodes that it joins to its network
        for (NetworkNode leafNode : leafNodes.values()) {
            if (addToNetwork.canAddLeafNode(leafNode)) {
                addToNetwork.addLeafNode(leafNode);
                newLeafNodes.add(leafNode);
            }
        }

        if (newLeafNodes.size() > 0) {
            notifyLeafNodesAdded(addToNetwork, newLeafNodes);
        }
    }

    public void addLeafBlock(NetworkNode networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            validateNoLeafOverlap(networkNode);

            for (SimpleNetwork network : networks) {
                if (network.canAddLeafNode(networkNode)) {
                    network.addLeafNode(networkNode);
                    notifyLeafNodesAdded(network, Collections.singleton(networkNode));
                }
            }

            // Check for new degenerated networks
            for (NetworkNode leafNode : leafNodes.values()) {
                if (SimpleNetwork.areNodesConnecting(networkNode, leafNode)) {
                    SimpleNetwork degenerateNetwork = SimpleNetwork.createDegenerateNetwork(networkNode, leafNode);
                    networks.add(degenerateNetwork);
                    notifyNetworkAdded(degenerateNetwork);
                    notifyLeafNodesAdded(degenerateNetwork, Sets.newHashSet(networkNode, leafNode));
                }
            }

            leafNodes.put(networkNode.location, networkNode);
        } finally {
            mutating = false;
        }
    }

    public void addLeafBlocks(Collection<NetworkNode> networkNodes) {
        // No optimizations can be made here
        for (NetworkNode networkNode : networkNodes) {
            addLeafBlock(networkNode);
        }
    }

    public void updateNetworkingBlock(NetworkNode oldNode, NetworkNode newNode) {
        removeNetworkingBlock(oldNode);
        addNetworkingBlock(newNode);
    }

    public void updateLeafBlock(NetworkNode oldNode, NetworkNode newNode) {
        removeLeafBlock(oldNode);
        addLeafBlock(newNode);
    }

    public void removeNetworkingBlock(NetworkNode networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            SimpleNetwork networkWithBlock = findNetworkWithNetworkingBlock(networkNode);

            if (networkWithBlock == null) {
                throw new IllegalStateException("Trying to remove a networking block that doesn't belong to any network");
            }

            networkingNodes.remove(networkNode.location, networkNode);

            // Naive implementation, just remove everything and start over
            // TODO: Improve to actually detects the branches of splits and build separate network for each disjunctioned
            // TODO: network
            Set<NetworkNode> networkingNodesToNotify = Sets.newHashSet(networkWithBlock.getNetworkingNodes());
            Set<NetworkNode> leafNodesToNotify = Sets.newHashSet(networkWithBlock.getLeafNodes());

            networkWithBlock.removeAllLeafNodes();
            notifyLeafNodesRemoved(networkWithBlock, leafNodesToNotify);
            networkWithBlock.removeAllNetworkingNodes();
            notifyNetworkingNodesRemoved(networkWithBlock, Collections.unmodifiableSet(networkingNodesToNotify));

            networks.remove(networkWithBlock);
            notifyNetworkRemoved(networkWithBlock);

            for (NetworkNode networkingNode : networkingNodesToNotify) {
                if (!networkingNode.equals(networkNode)) {
                    addNetworkingBlockInternal(networkingNode);
                }
            }
        } finally {
            mutating = false;
        }
    }

    public void removeNetworkingBlocks(Collection<NetworkNode> networkNodes) {
        if (networkNodes.size() == 0) {
            return;
        }
        // This performance improvement is needed until the split detection (above) is improved, after that it can be
        // removed
        validateNotMutating();
        mutating = true;
        try {
            Set<SimpleNetwork> affectedNetworks = Sets.newHashSet();
            for (NetworkNode networkNode : networkNodes) {
                final SimpleNetwork networkWithBlock = findNetworkWithNetworkingBlock(networkNode);
                if (networkWithBlock == null) {
                    throw new IllegalStateException("Trying to remove a networking block that doesn't belong to any network");
                }

                affectedNetworks.add(networkWithBlock);
                networkingNodes.remove(networkNode.location, networkNode);
            }

            List<Set<NetworkNode>> listOfNodesFromModifiedNetworks = Lists.newLinkedList();
            for (SimpleNetwork networkWithBlock : affectedNetworks) {
                Set<NetworkNode> leafNodesToNotify = Sets.newHashSet(networkWithBlock.getLeafNodes());
                Set<NetworkNode> networkingNodesToNotify = Sets.newHashSet(networkWithBlock.getNetworkingNodes());

                networkWithBlock.removeAllLeafNodes();
                notifyLeafNodesAdded(networkWithBlock, leafNodesToNotify);
                networkWithBlock.removeAllNetworkingNodes();
                notifyNetworkingNodesRemoved(networkWithBlock, Collections.unmodifiableSet(networkingNodesToNotify));

                networks.remove(networkWithBlock);
                notifyNetworkRemoved(networkWithBlock);
            }

            for (Set<NetworkNode> networkingNodesInModifiedNetwork : listOfNodesFromModifiedNetworks) {
                for (NetworkNode networkingNode : networkingNodesInModifiedNetwork) {
                    if (!networkNodes.contains(networkingNode)) {
                        addNetworkingBlockInternal(networkingNode);
                    }
                }
            }
        } finally {
            mutating = false;
        }
    }

    public void removeLeafBlock(NetworkNode networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            if (!leafNodes.remove(networkNode.location, networkNode)) {
                throw new IllegalArgumentException("Leaf node not found in the BlockNetwork");
            }
            final Iterator<SimpleNetwork> networkIterator = networks.iterator();
            while (networkIterator.hasNext()) {
                final SimpleNetwork network = networkIterator.next();
                if (network.hasLeafNode(networkNode)) {
                    boolean degenerate = network.removeLeafNode(networkNode);
                    if (!degenerate) {
                        notifyLeafNodesRemoved(network, Collections.singleton(networkNode));
                    } else {
                        NetworkNode onlyLeafNode = network.getLeafNodes().iterator().next();
                        notifyLeafNodesRemoved(network, Sets.newHashSet(networkNode, onlyLeafNode));

                        networkIterator.remove();
                        notifyNetworkRemoved(network);
                    }
                }
            }
        } finally {
            mutating = false;
        }
    }

    public void removeLeafBlocks(Collection<NetworkNode> networkNodes) {
        // No optimization can be made here
        for (NetworkNode networkNode : networkNodes) {
            removeLeafBlock(networkNode);
        }
    }

    public Collection<? extends Network> getNetworks() {
        return Collections.unmodifiableCollection(networks);
    }

    public boolean isNetworkActive(Network network) {
        return networks.contains(network);
    }

    private SimpleNetwork findNetworkWithNetworkingBlock(NetworkNode networkNode) {
        for (SimpleNetwork network : networks) {
            if (network.hasNetworkingNode(networkNode)) {
                return network;
            }
        }
        return null;
    }

    public Network getNetworkWithNetworkingBlock(NetworkNode networkNode) {
        return findNetworkWithNetworkingBlock(networkNode);
    }

    private void notifyNetworkAdded(SimpleNetwork network) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkAdded(network);
        }
    }

    private void notifyNetworkRemoved(SimpleNetwork network) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkRemoved(network);
        }
    }

    private void notifyNetworkingNodesAdded(SimpleNetwork network, Set<NetworkNode> networkingNodesToNotify) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkingNodesAdded(network, networkingNodesToNotify);
        }
    }

    private void notifyNetworkingNodesRemoved(SimpleNetwork network, Set<NetworkNode> networkingNodesToNotify) {
        for (NetworkTopologyListener listener : listeners) {
            listener.networkingNodesRemoved(network, networkingNodesToNotify);
        }
    }

    private void notifyLeafNodesAdded(SimpleNetwork network, Set<NetworkNode> leafNodesToNotify) {
        for (NetworkTopologyListener listener : listeners) {
            listener.leafNodesAdded(network, leafNodesToNotify);
        }
    }

    private void notifyLeafNodesRemoved(SimpleNetwork network, Set<NetworkNode> leafNodesToNotify) {
        for (NetworkTopologyListener listener : listeners) {
            listener.leafNodesRemoved(network, leafNodesToNotify);
        }
    }
}
