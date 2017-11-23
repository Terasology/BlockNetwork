/*
 * Copyright 2015 MovingBlocks
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
import org.terasology.math.geom.ImmutableVector3i;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * @deprecated Use EfficientBlockNetwork, this class will be removed.
 * @param <T>
 */
@Deprecated
public class BlockNetwork<T extends NetworkNode> {
    private Set<SimpleNetwork<T>> networks = Sets.newHashSet();
    private Multimap<ImmutableVector3i, T> leafNodes = HashMultimap.create();
    private Multimap<ImmutableVector3i, T> networkingNodes = HashMultimap.create();

    private Set<NetworkTopologyListener<T>> listeners = new HashSet<>();

    private boolean mutating;

    public void addTopologyListener(NetworkTopologyListener<T> listener) {
        listeners.add(listener);
    }

    public void removeTopologyListener(NetworkTopologyListener<T> listener) {
        listeners.remove(listener);
    }

    private void validateNotMutating() {
        if (mutating) {
            throw new IllegalStateException("Can't modify block network while modification is in progress");
        }
    }

    public void addNetworkingBlock(T networkNode) {
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

    public void addNetworkingBlocks(Collection<T> networkNodes) {
        // No major optimization possible here
        for (T networkNode : networkNodes) {
            addNetworkingBlock(networkNode);
        }
    }

    private void validateNoNetworkingOverlap(T networkNode) {
        for (T nodeAtSamePosition : networkingNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalStateException("There is a networking block at that position connecting to some of the same sides already");
            }
        }
    }

    private void validateNoLeafOverlap(T networkNode) {
        for (T nodeAtSamePosition : leafNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalStateException("There is a leaf block at that position connecting to some of the same sides already");
            }
        }
    }

    private void addNetworkingBlockInternal(T networkNode) {
        SimpleNetwork<T> addToNetwork = null;

        Set<T> networkingNodesToAdd = Sets.newHashSet();
        networkingNodesToAdd.add(networkNode);

        Set<T> newLeafNodes = Sets.newHashSet();

        // Try adding to existing networks
        final Iterator<SimpleNetwork<T>> networkIterator = networks.iterator();
        while (networkIterator.hasNext()) {
            final SimpleNetwork<T> network = networkIterator.next();
            if (network.canAddNetworkingNode(networkNode)) {
                if (addToNetwork == null) {
                    addToNetwork = network;
                } else {
                    Set<T> networkingNodesToNotify = Sets.newHashSet(network.getNetworkingNodes());
                    Set<T> leafNodesToNotify = Sets.newHashSet(network.getLeafNodes());

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
            SimpleNetwork<T> newNetwork = new SimpleNetwork<>();
            networks.add(newNetwork);
            notifyNetworkAdded(newNetwork);
            addToNetwork = newNetwork;
        }

        for (T networkingNode : networkingNodesToAdd) {
            addToNetwork.addNetworkingNode(networkingNode);
        }
        notifyNetworkingNodesAdded(addToNetwork, networkingNodesToAdd);

        for (T leafNode : newLeafNodes) {
            addToNetwork.addLeafNode(leafNode);
        }

        // Find all leaf nodes that it joins to its network
        for (T leafNode : leafNodes.values()) {
            if (addToNetwork.canAddLeafNode(leafNode)) {
                addToNetwork.addLeafNode(leafNode);
                newLeafNodes.add(leafNode);
            }
        }

        if (newLeafNodes.size() > 0) {
            notifyLeafNodesAdded(addToNetwork, newLeafNodes);
        }
    }

    public void addLeafBlock(T networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            validateNoLeafOverlap(networkNode);

            for (SimpleNetwork<T> network : networks) {
                if (network.canAddLeafNode(networkNode)) {
                    network.addLeafNode(networkNode);
                    notifyLeafNodesAdded(network, Collections.singleton(networkNode));
                }
            }

            // Check for new degenerated networks
            for (T leafNode : leafNodes.values()) {
                if (SimpleNetwork.areNodesConnecting(networkNode, leafNode)) {
                    SimpleNetwork<T> degenerateNetwork = SimpleNetwork.createDegenerateNetwork(networkNode, leafNode);
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

    public void addLeafBlocks(Collection<T> networkNodes) {
        // No optimizations can be made here
        for (T networkNode : networkNodes) {
            addLeafBlock(networkNode);
        }
    }

    public void updateNetworkingBlock(T oldNode, T newNode) {
        removeNetworkingBlock(oldNode);
        addNetworkingBlock(newNode);
    }

    public void updateLeafBlock(T oldNode, T newNode) {
        removeLeafBlock(oldNode);
        addLeafBlock(newNode);
    }

    public void removeNetworkingBlock(T networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            SimpleNetwork<T> networkWithBlock = findNetworkWithNetworkingBlock(networkNode);

            if (networkWithBlock == null) {
                throw new IllegalStateException("Trying to remove a networking block that doesn't belong to any network");
            }

            networkingNodes.remove(networkNode.location, networkNode);

            // Naive implementation, just remove everything and start over
            // TODO: Improve to actually detects the branches of splits and build separate network for each disjunctioned
            // TODO: network
            Set<T> networkingNodesToNotify = Sets.newHashSet(networkWithBlock.getNetworkingNodes());
            Set<T> leafNodesToNotify = Sets.newHashSet(networkWithBlock.getLeafNodes());

            networkWithBlock.removeAllLeafNodes();
            notifyLeafNodesRemoved(networkWithBlock, leafNodesToNotify);
            networkWithBlock.removeAllNetworkingNodes();
            notifyNetworkingNodesRemoved(networkWithBlock, Collections.unmodifiableSet(networkingNodesToNotify));

            networks.remove(networkWithBlock);
            notifyNetworkRemoved(networkWithBlock);

            for (T networkingNode : networkingNodesToNotify) {
                if (!networkingNode.equals(networkNode)) {
                    addNetworkingBlockInternal(networkingNode);
                }
            }
        } finally {
            mutating = false;
        }
    }

    public void removeNetworkingBlocks(Collection<T> networkNodes) {
        if (networkNodes.size() == 0) {
            return;
        }
        // This performance improvement is needed until the split detection (above) is improved, after that it can be
        // removed
        validateNotMutating();
        mutating = true;
        try {
            Set<SimpleNetwork<T>> affectedNetworks = Sets.newHashSet();
            for (T networkNode : networkNodes) {
                final SimpleNetwork<T> networkWithBlock = findNetworkWithNetworkingBlock(networkNode);
                if (networkWithBlock == null) {
                    throw new IllegalStateException("Trying to remove a networking block that doesn't belong to any network");
                }

                affectedNetworks.add(networkWithBlock);
                networkingNodes.remove(networkNode.location, networkNode);
            }

            List<Set<T>> listOfNodesFromModifiedNetworks = Lists.newLinkedList();
            for (SimpleNetwork<T> networkWithBlock : affectedNetworks) {
                Set<T> leafNodesToNotify = Sets.newHashSet(networkWithBlock.getLeafNodes());
                Set<T> networkingNodesToNotify = Sets.newHashSet(networkWithBlock.getNetworkingNodes());

                networkWithBlock.removeAllLeafNodes();
                notifyLeafNodesAdded(networkWithBlock, leafNodesToNotify);
                networkWithBlock.removeAllNetworkingNodes();
                notifyNetworkingNodesRemoved(networkWithBlock, Collections.unmodifiableSet(networkingNodesToNotify));

                networks.remove(networkWithBlock);
                notifyNetworkRemoved(networkWithBlock);
            }

            for (Set<T> networkingNodesInModifiedNetwork : listOfNodesFromModifiedNetworks) {
                for (T networkingNode : networkingNodesInModifiedNetwork) {
                    if (!networkNodes.contains(networkingNode)) {
                        addNetworkingBlockInternal(networkingNode);
                    }
                }
            }
        } finally {
            mutating = false;
        }
    }

    public void removeLeafBlock(T networkNode) {
        validateNotMutating();
        mutating = true;
        try {
            if (!leafNodes.remove(networkNode.location, networkNode)) {
                throw new IllegalArgumentException("Leaf node not found in the BlockNetwork");
            }
            final Iterator<SimpleNetwork<T>> networkIterator = networks.iterator();
            while (networkIterator.hasNext()) {
                final SimpleNetwork<T> network = networkIterator.next();
                if (network.hasLeafNode(networkNode)) {
                    boolean degenerate = network.removeLeafNode(networkNode);
                    if (!degenerate) {
                        notifyLeafNodesRemoved(network, Collections.singleton(networkNode));
                    } else {
                        T onlyLeafNode = network.getLeafNodes().iterator().next();
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

    public void removeLeafBlocks(Collection<T> networkNodes) {
        // No optimization can be made here
        for (T networkNode : networkNodes) {
            removeLeafBlock(networkNode);
        }
    }

    public Collection<? extends Network<T>> getNetworks() {
        return Collections.unmodifiableCollection(networks);
    }

    public boolean isNetworkActive(Network<T> network) {
        return networks.contains(network);
    }

    private SimpleNetwork<T> findNetworkWithNetworkingBlock(T networkNode) {
        for (SimpleNetwork<T> network : networks) {
            if (network.hasNetworkingNode(networkNode)) {
                return network;
            }
        }
        return null;
    }

    public Network getNetworkWithNetworkingBlock(T networkNode) {
        return findNetworkWithNetworkingBlock(networkNode);
    }

    private void notifyNetworkAdded(SimpleNetwork<T> network) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.networkAdded(network);
        }
    }

    private void notifyNetworkRemoved(SimpleNetwork<T> network) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.networkRemoved(network);
        }
    }

    private void notifyNetworkingNodesAdded(SimpleNetwork<T> network, Set<T> networkingNodesToNotify) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.networkingNodesAdded(network, networkingNodesToNotify);
        }
    }

    private void notifyNetworkingNodesRemoved(SimpleNetwork<T> network, Set<T> networkingNodesToNotify) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.networkingNodesRemoved(network, networkingNodesToNotify);
        }
    }

    private void notifyLeafNodesAdded(SimpleNetwork<T> network, Set<T> leafNodesToNotify) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.leafNodesAdded(network, leafNodesToNotify);
        }
    }

    private void notifyLeafNodesRemoved(SimpleNetwork<T> network, Set<T> leafNodesToNotify) {
        for (NetworkTopologyListener<T> listener : listeners) {
            listener.leafNodesRemoved(network, leafNodesToNotify);
        }
    }
}
