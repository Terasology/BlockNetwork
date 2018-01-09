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
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import org.terasology.math.geom.Vector3i;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * A class that manages the creation, removal, merging, and splitting of networks of connected blocks.
 *
 * @param <T> The type of node in this network.
 */
public class EfficientBlockNetwork<T extends NetworkNode> {
    private Set<EfficientNetwork<T>> networks = Sets.newHashSet();
    private Multimap<ImmutableBlockLocation, T> leafNodes = HashMultimap.create();
    private Multimap<ImmutableBlockLocation, T> networkingNodes = HashMultimap.create();

    private Set<EfficientNetworkTopologyListener<T>> listeners = new HashSet<>();

    private boolean mutating;

    /**
     * Adds a given listener to the listeners to be notified of topology events of this network.
     * <p>
     * Listeners are notified when nodes are added or removed, and when networks are added, removed, split, or merged.
     * If the given listener is equal to one already added, attempting to add it again does nothing.
     *
     * @param listener The listener to add.
     * @see EfficientNetworkTopologyListener
     */
    public void addTopologyListener(EfficientNetworkTopologyListener<T> listener) {
        listeners.add(listener);
    }

    /**
     * Removes a given listener from the listeners to be notified of topology events of this network.
     *
     * @param listener The listener to remove.
     */
    public void removeTopologyListener(EfficientNetworkTopologyListener<T> listener) {
        listeners.remove(listener);
    }

    /**
     * Adds a given networking block to this network.
     *
     * @param networkNode The networking block to add.
     * @param reason The reason for addition.
     * @throws IllegalArgumentException if the networking block shares a connection side with a networking block in this network.
     */
    public void addNetworkingBlock(T networkNode, NetworkChangeReason reason) {
        addNetworkingBlocks(Collections.singleton(networkNode), reason);
    }

    /**
     * Gets the leaf nodes at a given location.
     *
     * @param location The location to query.
     * @return A collection of leaf nodes at <code>location</code>.
     */
    public Collection<T> getLeafNodesAt(Vector3i location) {
        return leafNodes.get(new ImmutableBlockLocation(location));
    }

    /**
     * Gets the networking nodes at a given location.
     *
     * @param location The location to query.
     * @return A collection of leaf nodes at <code>location</code>.
     */
    public Collection<T> getNetworkingNodesAt(Vector3i location) {
        return networkingNodes.get(new ImmutableBlockLocation(location));
    }

    /**
     * Checks if this block network contains a given leaf node.
     *
     * @param node The node to check.
     * @return true if <code>node</code> is a leaf node in this block network, false otherwise.
     */
    public boolean containsLeafNode(T node) {
        return leafNodes.containsValue(node);
    }

    /**
     * Checks if this block network contains a given networking node.
     *
     * @param node The node to check
     * @return true if <code>node</code> is a networking node in this block network, false otherwise.
     */
    public boolean containsNetworkingNode(T node) {
        return networkingNodes.containsValue(node);
    }

    /**
     * Adds a given collection of networking blocks to this network.
     *
     * @param networkNodes The collection of networking blocks to add.
     * @param reason The reason for addition.
     * @throws IllegalArgumentException if a networking block in <code>networkNodes</code> shares a connection side with a networking block in this network.
     */
    public void addNetworkingBlocks(Collection<T> networkNodes, NetworkChangeReason reason) {
        validateNotMutating();
        mutating = true;
        try {
            for (T networkNode : networkNodes) {
                validateNoNetworkingOverlap(networkNode);
            }
            for (T networkNode : networkNodes) {
                networkingNodes.put(networkNode.location, networkNode);
            }

            Set<T> nodesToAdd = Sets.newHashSet(networkNodes);

            // First try to merge any nodes that are merge-able
            while (!nodesToAdd.isEmpty()) {
                boolean changed = false;
                Iterator<T> iterator = nodesToAdd.iterator();
                while (iterator.hasNext()) {
                    T node = iterator.next();

                    Set<EfficientNetwork<T>> networksThatAcceptNode = getNetworksThatAcceptNode(node);
                    if (networksThatAcceptNode.size() > 0) {
                        addNetworkingBlockInternal(node, reason, networksThatAcceptNode);
                        iterator.remove();
                        changed = true;
                    }
                }

                // No new node has been merged in this pass, so just create a new network with the first node
                // and continue trying to merge nodes into possibly new networks
                if (!changed) {
                    Iterator<T> nodeIterator = nodesToAdd.iterator();
                    createNewNetworkWithNode(nodeIterator.next(), reason);
                    nodeIterator.remove();
                }
            }
        } finally {
            mutating = false;
        }
    }

    private void validateNoNetworkingOverlap(T networkNode) {
        for (T nodeAtSamePosition : networkingNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalArgumentException("There is a networking block at that position connecting to some of the same sides already");
            }
        }
    }

    private void validateNoLeafOverlap(T networkNode) {
        for (T nodeAtSamePosition : leafNodes.get(networkNode.location)) {
            if ((nodeAtSamePosition.connectionSides & networkNode.connectionSides) > 0) {
                throw new IllegalArgumentException("There is a leaf block at that position connecting to some of the same sides already");
            }
        }
    }

    private void addNetworkingBlockInternal(T networkNode, NetworkChangeReason reason, Set<EfficientNetwork<T>> networksThatAcceptNode) {
        int acceptingNetworkCount = networksThatAcceptNode.size();
        if (acceptingNetworkCount == 0) {
            // This block is not joining any network, just create a new network for it
            // and add any leaf blocks it might connect to
            createNewNetworkWithNode(networkNode, reason);
        } else if (acceptingNetworkCount == 1) {
            // This block is joining exactly one network, so just add it to it
            // and add any leaf blocks it might connect into the network
            EfficientNetwork<T> network = networksThatAcceptNode.iterator().next();
            joinNodeIntoNetwork(networkNode, reason, network);
        } else {
            // This block is merging multiple networks together, create one
            // super network from the blocks of all the networks
            mergeNetworksWithNode(networkNode, reason, networksThatAcceptNode);
        }
    }

    private void mergeNetworksWithNode(T networkNode, NetworkChangeReason reason, Set<EfficientNetwork<T>> networksToMerge) {
        EfficientNetwork<T> mergeResult = new EfficientNetwork<>();

        for (EfficientNetwork<T> networkToMerge : networksToMerge) {
            for (T oldNode : networkToMerge.getNetworkingNodes()) {
                mergeResult.addNetworkingNode(oldNode);
            }
            networks.remove(networkToMerge);
        }
        mergeResult.addNetworkingNode(networkNode);

        // Find all leaf nodes that it joins to its network
        for (T leafNode : leafNodes.values()) {
            if (mergeResult.canAddLeafNode(leafNode)) {
                mergeResult.addLeafNode(leafNode);
            }
        }

        networks.add(mergeResult);
        notifyNetworksMerged(networksToMerge, mergeResult, reason);
    }

    private void joinNodeIntoNetwork(T networkNode, NetworkChangeReason reason, EfficientNetwork<T> network) {
        network.addNetworkingNode(networkNode);
        notifyNetworkingNodesAdded(network, Collections.singleton(networkNode), reason);

        Set<T> leafNodesAdded = Sets.newHashSet();
        // Find all leaf nodes that it joins to its network
        for (T leafNode : leafNodes.values()) {
            if (network.canAddLeafNode(leafNode)) {
                network.addLeafNode(leafNode);
                leafNodesAdded.add(leafNode);
            }
        }
        if (leafNodesAdded.size() > 0) {
            notifyLeafNodesAdded(network, leafNodesAdded, reason);
        }
    }

    private void createNewNetworkWithNode(T networkNode, NetworkChangeReason reason) {
        EfficientNetwork<T> newNetwork = new EfficientNetwork<>();
        newNetwork.addNetworkingNode(networkNode);

        // Find all leaf nodes that it joins to its network
        for (T leafNode : leafNodes.values()) {
            if (newNetwork.canAddLeafNode(leafNode)) {
                newNetwork.addLeafNode(leafNode);
            }
        }

        networks.add(newNetwork);
        notifyNetworkAdded(newNetwork, reason);
    }

    private Set<EfficientNetwork<T>> getNetworksThatAcceptNode(T networkNode) {
        Set<EfficientNetwork<T>> acceptingNetworks = Sets.newHashSet();

        // Check which networks accept it
        for (EfficientNetwork<T> network : networks) {
            if (network.canAddNetworkingNode(networkNode)) {
                acceptingNetworks.add(network);
            }
        }
        return acceptingNetworks;
    }

    /**
     * Adds a given leaf block to this network.
     *
     * @param networkNode The leaf block to add.
     * @param reason The reason for addition.
     * @throws IllegalArgumentException if the leaf block shares a connection side with a leaf block in this network.
     */
    public void addLeafBlock(T networkNode, NetworkChangeReason reason) {
        addLeafBlocks(Collections.singleton(networkNode), reason);
    }

    /**
     * Adds a given collection of leaf blocks to this network.
     *
     * @param networkNodes The collection of leaf blocks to add.
     * @param reason The reason for addition.
     * @throws IllegalArgumentException if a leaf block in <code>networkNodes</code> shares a connection side with a leaf block in this network.
     */
    public void addLeafBlocks(Collection<T> networkNodes, NetworkChangeReason reason) {
        // No optimizations can be made here
        validateNotMutating();
        mutating = true;
        try {
            for (T networkNode : networkNodes) {
                validateNoLeafOverlap(networkNode);
            }

            for (T networkNode : networkNodes) {
                addLeafBlockInternal(networkNode, reason);
            }
        } finally {
            mutating = false;
        }
    }

    private void addLeafBlockInternal(T networkNode, NetworkChangeReason reason) {
        leafNodes.put(networkNode.location, networkNode);

        for (EfficientNetwork<T> network : networks) {
            if (network.canAddLeafNode(networkNode)) {
                network.addLeafNode(networkNode);
                notifyLeafNodesAdded(network, Collections.singleton(networkNode), reason);
            }
        }

        // Check for new degenerated networks
        for (T leafNode : leafNodes.values()) {
            if (EfficientNetwork.areNodesConnecting(networkNode, leafNode)) {
                EfficientNetwork<T> degenerateNetwork = EfficientNetwork.createLeafOnlyNetwork(networkNode, leafNode);
                networks.add(degenerateNetwork);
                notifyNetworkAdded(degenerateNetwork, reason);
            }
        }
    }

    /**
     * Removes a given networking block from this network.
     *
     * @param networkNode The networking block to remove.
     * @param reason The reason for removal.
     * @throws IllegalArgumentException if <code>networkNode</code> is not in this block network.
     */
    public void removeNetworkingBlock(T networkNode, NetworkChangeReason reason) {
        removeNetworkingBlocks(Collections.singleton(networkNode), reason);
    }

    /**
     * Removes a given collection of networking blocks from this network.
     *
     * @param networkNodes The collection of networking blocks to remove.
     * @param reason The reason for removal.
     * @throws IllegalArgumentException if a networking block in <code>networkNodes</code> is not in this block network.
     */
    public void removeNetworkingBlocks(Collection<T> networkNodes, NetworkChangeReason reason) {
        if (networkNodes.size() == 0) {
            return;
        }

        validateNotMutating();
        mutating = true;
        try {
            for (T networkNode : networkNodes) {
                if (!networkingNodes.containsEntry(networkNode.location, networkNode)) {
                    throw new IllegalArgumentException("Trying to remove a leaf block that is not in the network.");
                }
            }

            for (T networkNode : networkNodes) {
                removeNetworkingBlockInternal(networkNode, reason);
            }
        } finally {
            mutating = false;
        }
    }

    private void removeNetworkingBlockInternal(T networkNode, NetworkChangeReason reason) {
        EfficientNetwork<T> networkWithBlock = findNetworkWithNetworkingBlock(networkNode);

        networkingNodes.remove(networkNode.location, networkNode);

        EfficientNetwork.NetworkingNodeRemovalResult<T> removalResult = networkWithBlock.removeNetworkingNodeOrSplit(networkNode);
        if (removalResult.removesNetwork) {
            networks.remove(networkWithBlock);
            notifyNetworkRemoved(networkWithBlock, reason);
        } else if (removalResult.removedLeafNodes != null) {
            notifyNetworkingNodesRemoved(networkWithBlock, Collections.singleton(networkNode), reason);
            if (removalResult.removedLeafNodes.size() > 0) {
                notifyLeafNodesRemoved(networkWithBlock, removalResult.removedLeafNodes, reason);
            }
        } else {
            networks.remove(networkWithBlock);
            networks.addAll(removalResult.splitResult);
            notifyNetworkSplit(networkWithBlock, removalResult.splitResult, reason);
        }
    }

    /**
     * Removes a given leaf block from this network.
     *
     * @param networkNode The leaf block to remove.
     * @param reason The reason for removal.
     * @throws IllegalArgumentException if <code>networkNode</code> is not in this block network.
     */
    public void removeLeafBlock(T networkNode, NetworkChangeReason reason) {
        removeLeafBlocks(Collections.singleton(networkNode), reason);
    }

    /**
     * Removes a given collection of leaf blocks from this network.
     *
     * @param networkNodes The collection of leaf blocks to remove.
     * @param reason The reason for removal.
     * @throws IllegalArgumentException if a leaf block in <code>networkNodes</code> is not in this block network.
     */
    public void removeLeafBlocks(Collection<T> networkNodes, NetworkChangeReason reason) {
        // No optimization can be made here
        validateNotMutating();
        mutating = true;
        try {
            for (T networkNode : networkNodes) {
                if (!leafNodes.containsEntry(networkNode.location, networkNode)) {
                    throw new IllegalArgumentException("Trying to remove a leaf block that is not in the network.");
                }
            }

            for (T networkNode : networkNodes) {
                removeLeafBlockInternal(networkNode, reason);
            }
        } finally {
            mutating = false;
        }
    }

    private void removeLeafBlockInternal(T networkNode, NetworkChangeReason reason) {
        leafNodes.remove(networkNode.location, networkNode);

        final Iterator<EfficientNetwork<T>> networkIterator = networks.iterator();
        while (networkIterator.hasNext()) {
            final EfficientNetwork<T> network = networkIterator.next();
            if (network.hasLeafNode(networkNode)) {
                if (network.isTwoLeafNetwork()) {
                    networkIterator.remove();
                    notifyNetworkRemoved(network, reason);
                } else {
                    network.removeLeafNode(networkNode);
                    notifyLeafNodesRemoved(network, Collections.singleton(networkNode), reason);
                }
            }
        }
    }

    /**
     * Gets underlying networks managed by this object.
     *
     * @return A collection of networks.
     */
    public Collection<? extends Network2<T>> getNetworks() {
        return Collections.unmodifiableCollection(networks);
    }

    /**
     * Checks if a given network is managed by this object.
     *
     * @param network The network to check.
     * @return true if <code>network</code> is a network managed by this object, false otherwise.
     */
    public boolean isNetworkActive(Network2<T> network) {
        return networks.contains(network);
    }

    private EfficientNetwork<T> findNetworkWithNetworkingBlock(T networkNode) {
        for (EfficientNetwork<T> network : networks) {
            if (network.hasNetworkingNode(networkNode)) {
                return network;
            }
        }
        return null;
    }

    /**
     * Finds a network managed by this object that contains a given networking node as a node.
     *
     * @param networkNode The networking node to find the network of.
     * @return The network that contains <code>networkNode</code> as a networking node, or null if no network managed by this object contains <code>networkNode</code> as a networking node.
     */
    public Network2 getNetworkWithNetworkingBlock(T networkNode) {
        return findNetworkWithNetworkingBlock(networkNode);
    }

    private void validateNotMutating() {
        if (mutating) {
            throw new IllegalArgumentException("Can't modify block network while modification is in progress");
        }
    }

    private void notifyNetworkAdded(EfficientNetwork<T> network, NetworkChangeReason reason) {
        for (EfficientNetworkTopologyListener<T> listener : listeners) {
            listener.networkAdded(network, reason);
        }
    }

    private void notifyNetworkSplit(EfficientNetwork<T> oldNetwork, Set<EfficientNetwork<T>> newNetworks, NetworkChangeReason reason) {
        for (EfficientNetworkTopologyListener<T> listener : listeners) {
            listener.networkSplit(oldNetwork, newNetworks, reason);
        }
    }

    private void notifyNetworksMerged(Set<EfficientNetwork<T>> oldNetworks, EfficientNetwork<T> newNetwork, NetworkChangeReason reason) {
        for (EfficientNetworkTopologyListener<T> listener : listeners) {
            listener.networksMerged(oldNetworks, newNetwork, reason);
        }
    }

    private void notifyNetworkRemoved(EfficientNetwork<T> network, NetworkChangeReason reason) {
        for (EfficientNetworkTopologyListener<T> listener : listeners) {
            listener.networkRemoved(network, reason);
        }
    }

    private void notifyNetworkingNodesAdded(EfficientNetwork<T> network, Set<T> networkingNodesToNotify, NetworkChangeReason reason) {
        if (networkingNodesToNotify.size() > 0) {
            for (EfficientNetworkTopologyListener<T> listener : listeners) {
                listener.networkingNodesAdded(network, networkingNodesToNotify, reason);
            }
        }
    }

    private void notifyNetworkingNodesRemoved(EfficientNetwork<T> network, Set<T> networkingNodesToNotify, NetworkChangeReason reason) {
        if (networkingNodesToNotify.size() > 0) {
            for (EfficientNetworkTopologyListener<T> listener : listeners) {
                listener.networkingNodesRemoved(network, networkingNodesToNotify, reason);
            }
        }
    }

    private void notifyLeafNodesAdded(EfficientNetwork<T> network, Set<T> leafNodesToNotify, NetworkChangeReason reason) {
        if (leafNodesToNotify.size() > 0) {
            for (EfficientNetworkTopologyListener<T> listener : listeners) {
                listener.leafNodesAdded(network, leafNodesToNotify, reason);
            }
        }
    }

    private void notifyLeafNodesRemoved(EfficientNetwork<T> network, Set<T> leafNodesToNotify, NetworkChangeReason reason) {
        if (leafNodesToNotify.size() > 0) {
            for (EfficientNetworkTopologyListener<T> listener : listeners) {
                listener.leafNodesRemoved(network, leafNodesToNotify, reason);
            }
        }
    }
}
