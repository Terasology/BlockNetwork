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

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.terasology.blockNetwork.traversal.BreadthFirstTraversal;
import org.terasology.blockNetwork.traversal.BreadthFirstTraversalWithPath;
import org.terasology.blockNetwork.traversal.TraversalResult;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents one network of nodes, where each node is somehow connected to another within the network.
 * <p>
 * Network contains following node types:
 * - networking nodes - nodes that are a back-bone of a network. These allow to connect multiple nodes in the network.
 * A networking node "conducts" the "signal" of the network to nodes defined in the "connectingOnSides" nodes in its
 * vicinity.
 * - leaf nodes - nodes that are only receiving or producing a signal, and do not themselves "conduct" it to other nodes.
 * <p>
 * A couple of non-obvious facts:
 * 1. The same node (defined as location) cannot be both a networking node and a leaf node in the same network.
 * 2. The same leaf node can be a member of multiple disjunctive networks (different network on each side).
 * 3. A valid network can have no networking nodes at all, and exactly two leaf nodes (neighbouring leaf nodes).
 */
public class EfficientNetwork<T extends NetworkNode> implements Network2<T> {
    private static final boolean SANITY_CHECK = false;
    private SetMultimap<ImmutableBlockLocation, T> networkingNodes = HashMultimap.create();
    private SetMultimap<ImmutableBlockLocation, T> leafNodes = HashMultimap.create();

    // Distance cache
    private Map<TwoNetworkNodes, Integer> distanceCache = Maps.newHashMap();
    private Map<TwoNetworkNodes, List<T>> shortestRouteCache = Maps.newHashMap();

    /**
     * Creates a network containing only two given nodes as leaves.
     *
     * @param networkNode1 The first node
     * @param networkNode2 The second node
     * @param <T> The type of the nodes
     * @return A network containing the two nodes as leaves
     */
    public static <T extends NetworkNode> EfficientNetwork<T> createLeafOnlyNetwork(
            T networkNode1,
            T networkNode2) {
        if (!areNodesConnecting(networkNode1, networkNode2)) {
            throw new IllegalArgumentException("These two nodes are not connected");
        }

        EfficientNetwork<T> network = new EfficientNetwork<T>();
        network.leafNodes.put(networkNode1.location, networkNode1);
        network.leafNodes.put(networkNode2.location, networkNode2);
        return network;
    }

    /**
     * Checks if this network consists of only two leaves.
     *
     * @return true if this network has two leaves and no networking nodes, otherwise false.
     */
    public boolean isTwoLeafNetwork() {
        return networkingNodes.size() == 0 && leafNodes.size() == 2;
    }

    /**
     * Adds a networking node to the network.
     *
     * @param networkNode Definition of the networking node position and connecting sides.
     */
    public void addNetworkingNode(T networkNode) {
        if (SANITY_CHECK && !canAddNetworkingNode(networkNode)) {
            throw new IllegalArgumentException("Unable to add this node to network");
        }
        networkingNodes.put(networkNode.location, networkNode);
        distanceCache.clear();
        shortestRouteCache.clear();
    }

    /**
     * Adds a leaf node to the network.
     *
     * @param networkNode Definition of the leaf node position and connecting sides.
     */
    public void addLeafNode(T networkNode) {
        if (SANITY_CHECK && (!canAddLeafNode(networkNode) || isEmptyNetwork())) {
            throw new IllegalArgumentException("Unable to add this node to network");
        }
        leafNodes.put(networkNode.location, networkNode);
        distanceCache.clear();
        shortestRouteCache.clear();
    }

    /**
     * Returns the network size - a number of nodes it spans. If the same node is added twice with different
     * connecting sides, it is counted twice.
     *
     * @return The sum of networking nodes and leaf nodes (count).
     */
    @Override
    public int getNetworkSize() {
        return networkingNodes.size() + leafNodes.size();
    }

    /**
     * Removes a leaf node from the network.
     *
     * @param networkingNode Definition of the leaf node position and connecting sides.
     */
    public void removeLeafNode(T networkingNode) {
        // Removal of a leaf node cannot split the network, so it's just safe to remove it
        // We just need to check, if after removal of the node, network becomes degenerated, if so - we need
        // to signal that the network is no longer valid and should be removed.
        final boolean changed = leafNodes.remove(networkingNode.location, networkingNode);
        if (!changed) {
            throw new IllegalArgumentException("Tried to remove a node that is not in the network");
        }

        distanceCache.clear();
        shortestRouteCache.clear();
    }

    private T findNetworkScanStartingNode(Set<T> excludingNode) {
        for (T node : networkingNodes.values()) {
            if (!excludingNode.contains(node)) {
                return node;
            }
        }

        return null;
    }

    /**
     * Removes a given networking node from this network, potentially splitting the network.
     *
     * @param networkNode The node to remove.
     * @return An object representing the result of the removal.
     * @throws IllegalArgumentException if <code>networkNode</code> is not a networking node of this network.
     * @see NetworkingNodeRemovalResult
     */
    public NetworkingNodeRemovalResult<T> removeNetworkingNodeOrSplit(T networkNode) {
        if (!networkingNodes.containsEntry(networkNode.location, networkNode)) {
            throw new IllegalArgumentException("Tried to remove a node that is not in the network");
        }

        Set<T> networkNodesUnavailable = new HashSet<>();
        networkNodesUnavailable.add(networkNode);

        Set<EfficientNetwork<T>> splitResult = Sets.newHashSet();
        while (true) {
            Set<T> newNetworkNodes = new HashSet<>();

            T start = findNetworkScanStartingNode(networkNodesUnavailable);
            if (start == null) {
                break;
            }
            newNetworkNodes.add(start);

            traverseBreadthFirst(start,
                    new BreadthFirstTraversal<T, Void, Void>() {
                        @Override
                        public TraversalResult<T, Void, Void> visitNode(T node, Void parentValue) {
                            newNetworkNodes.add(node);
                            return TraversalResult.continuePath(null);
                        }
                    }, new Predicate<T>() {
                        @Override
                        public boolean apply(@Nullable T input) {
                            return networkingNodes.containsValue(input) &&
                                    !networkNodesUnavailable.contains(input);
                        }
                    }, null, null);

            networkNodesUnavailable.addAll(newNetworkNodes);

            EfficientNetwork<T> newNetwork = new EfficientNetwork<>();
            for (T newNetworkNode : newNetworkNodes) {
                newNetwork.addNetworkingNode(newNetworkNode);
            }

            for (T t : leafNodes.values()) {
                if (newNetwork.canAddLeafNode(t)) {
                    newNetwork.addLeafNode(t);
                }
            }

            splitResult.add(newNetwork);
        }

        if (splitResult.size() == 0) {
            return NetworkingNodeRemovalResult.removeNetwork();
        } else if (splitResult.size() == 1) {
            // Just need to remove the nodes...
            networkingNodes.remove(networkNode.location, networkNode);

            EfficientNetwork<T> newNetwork = splitResult.iterator().next();
            Collection<T> newLeafNodes = newNetwork.getLeafNodes();

            Set<T> removedLeafNodes = Sets.newHashSet();
            for (T t : leafNodes.values()) {
                if (!newLeafNodes.contains(t)) {
                    removedLeafNodes.add(t);
                }
            }

            for (T removedLeafNode : removedLeafNodes) {
                leafNodes.remove(removedLeafNode.location, removedLeafNode);
            }

            distanceCache.clear();
            shortestRouteCache.clear();

            return NetworkingNodeRemovalResult.removeLeafNodes(removedLeafNodes);
        } else {
            return NetworkingNodeRemovalResult.splitNetwork(splitResult);
        }
    }

    @Override
    public Collection<T> getNetworkingNodes() {
        return Collections.unmodifiableCollection(networkingNodes.values());
    }

    @Override
    public Collection<T> getLeafNodes() {
        return Collections.unmodifiableCollection(leafNodes.values());
    }

    /**
     * Checks if two given nodes are directly connecting.
     *
     * @param node1 The first node
     * @param node2 The second node
     * @return true if the two nodes are directly connecting, output to input. Otherwise, false.
     */
    public static boolean areNodesConnecting(NetworkNode node1, NetworkNode node2) {
        for (Side side : SideBitFlag.getSides(node1.inputSides)) {
            final ImmutableBlockLocation possibleConnectedLocation = node1.location.move(side);
            if (node2.location.equals(possibleConnectedLocation) && SideBitFlag.hasSide(node2.outputSides, side.reverse())) {
                return true;
            }
        }
        for (Side side : SideBitFlag.getSides(node1.outputSides)) {
            final ImmutableBlockLocation possibleConnectedLocation = node1.location.move(side);
            if (node2.location.equals(possibleConnectedLocation) && SideBitFlag.hasSide(node2.inputSides, side.reverse())) {
                return true;
            }
        }
        return false;
    }

    /**
     * If this network can connect to node at the location specified with the specified connecting sides.
     *
     * @param networkNode Definition of the networking node position and connecting sides.
     * @return If the networking node can be added to the network (connects to it).
     */
    public boolean canAddNetworkingNode(T networkNode) {
        if (isEmptyNetwork()) {
            return true;
        }
        if (networkingNodes.containsValue(networkNode) || leafNodes.containsValue(networkNode)) {
            return false;
        }
        return canConnectToNetworkingNode(networkNode);
    }

    /**
     * If this network can connect to node at the location specified with the specified connecting sides.
     *
     * @param networkNode Definition of the leaf node position and connecting sides.
     * @return If the leaf node can be added to the network (connects to it).
     */
    public boolean canAddLeafNode(T networkNode) {
        if (isEmptyNetwork()) {
            return false;
        }
        if (networkingNodes.containsValue(networkNode) || leafNodes.containsValue(networkNode)) {
            return false;
        }

        return canConnectToNetworkingNode(networkNode);
    }

    private boolean canConnectToNetworkingNode(T networkNode) {
        for (Side connectingOnSide : SideBitFlag.getSides(networkNode.inputSides)) {
            final ImmutableBlockLocation possibleConnectionLocation = networkNode.location.move(connectingOnSide);
            for (NetworkNode possibleConnectedNode : networkingNodes.get(possibleConnectionLocation)) {
                if (SideBitFlag.hasSide(possibleConnectedNode.outputSides, connectingOnSide.reverse())) {
                    return true;
                }
            }
        }
        for (Side connectingOnSide : SideBitFlag.getSides(networkNode.outputSides)) {
            final ImmutableBlockLocation possibleConnectionLocation = networkNode.location.move(connectingOnSide);
            for (NetworkNode possibleConnectedNode : networkingNodes.get(possibleConnectionLocation)) {
                if (SideBitFlag.hasSide(possibleConnectedNode.inputSides, connectingOnSide.reverse())) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean hasNetworkingNode(T networkNode) {
        return networkingNodes.containsValue(networkNode);
    }

    @Override
    public boolean hasLeafNode(T networkNode) {
        return leafNodes.containsValue(networkNode);
    }

    @Override
    public int getDistance(T from, T to, int maxToSearch) {
        validateAnyOfTheNodesInNetwork(from, to);

        return getDistanceInternal(from, to, maxToSearch);
    }

    private int getDistanceInternal(NetworkNode from, NetworkNode to, int maxToSearch) {
        TwoNetworkNodes nodePair = new TwoNetworkNodes(from, to);
        final Integer cachedDistance = distanceCache.get(nodePair);
        if (cachedDistance != null) {
            if (cachedDistance > maxToSearch) {
                return -1;
            }
            return cachedDistance;
        }

        if (from.equals(to)) {
            return 0;
        }

        if (EfficientNetwork.areNodesConnecting(from, to)) {
            return 1;
        }

        return traverseBreadthFirstInternal(from,
                new BreadthFirstTraversal<T, Integer, Integer>() {
                    @Override
                    public TraversalResult<T, Integer, Integer> visitNode(T node, Integer distanceSearched) {
                        int distance = distanceSearched;
                        distance++;
                        if (distance > maxToSearch) {
                            return TraversalResult.returnResult(-1);
                        }

                        if (EfficientNetwork.areNodesConnecting(node, to)) {
                            distanceCache.put(
                                    new TwoNetworkNodes(from, to),
                                    distance);
                            return TraversalResult.returnResult(distance);
                        }

                        return TraversalResult.continuePath(distance);
                    }
                }, Predicates.<T>alwaysTrue(), -1, 1);
    }

    @Override
    public int getDistanceWithSide(T from, T to, Side toSide, int maxToSearch) {
        validateAnyOfTheNodesInNetwork(from, to);

        NetworkNode destination = new NetworkNode(to.location.toVector3i(), (byte) 0, SideBitFlag.getSide(toSide));
        return getDistanceInternal(from, destination, maxToSearch);
    }

    @Override
    public List<T> findShortestRoute(T from, T to) {
        TwoNetworkNodes nodePair = new TwoNetworkNodes(from, to);
        final List<T> cachedRoute = shortestRouteCache.get(nodePair);
        if (cachedRoute != null) {
            return Collections.unmodifiableList(cachedRoute);
        }

        validateAnyOfTheNodesInNetwork(from, to);

        if (from.equals(to)) {
            return Arrays.asList(from);
        }

        if (EfficientNetwork.areNodesConnecting(from, to)) {
            return Arrays.asList(from, to);
        }

        return traverseBreadthFirstWithPath(from,
                new BreadthFirstTraversalWithPath<T, List<T>, Void>() {
                    @Override
                    public TraversalResult<T, List<T>, Void> visitNode(T node, Void parentValue, List<T> path) {
                        if (EfficientNetwork.areNodesConnecting(node, to)) {
                            List<T> route = new LinkedList<>(path);
                            route.add(to);

                            shortestRouteCache.put(
                                    new TwoNetworkNodes(from, to),
                                    route);
                            return TraversalResult.<T, List<T>, Void>returnResult(route);
                        }
                        return TraversalResult.continuePath(null);
                    }
                }, Predicates.<T>alwaysTrue(), null, null);
    }

    private void validateAnyOfTheNodesInNetwork(T from, T to) {
        if (!hasNetworkingNode(from) && !hasLeafNode(from)
                || !hasNetworkingNode(to) && !hasLeafNode(to)) {
            throw new IllegalArgumentException("Cannot test nodes not in network");
        }
    }

    @Override
    public byte getLeafSidesInNetwork(T networkNode) {
        if (!hasLeafNode(networkNode)) {
            throw new IllegalArgumentException("Cannot test nodes not in network");
        }

        if (networkingNodes.size() == 0) {
            // Degenerated network
            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.inputSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : leafNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.outputSides, connectingOnSide.reverse())) {
                        return SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }

            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.outputSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : leafNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.inputSides, connectingOnSide.reverse())) {
                        return SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }

            return 0;
        } else {
            byte result = 0;
            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.outputSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : networkingNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.inputSides, connectingOnSide.reverse())) {
                        result += SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }
            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.inputSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : networkingNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.outputSides, connectingOnSide.reverse())) {
                        result += SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }

            return result;
        }
    }

    private boolean isDegeneratedNetwork() {
        return networkingNodes.isEmpty() && leafNodes.size() == 1;
    }

    private boolean isEmptyNetwork() {
        return networkingNodes.isEmpty() && leafNodes.isEmpty();
    }

    @Override
    public <U, V> U traverseBreadthFirstWithPath(T from, BreadthFirstTraversalWithPath<T, U, V> traversal, Predicate<T> defaultPredicate,
                                                 U defaultResult, V initialValue) {
        if (!hasNetworkingNode(from) && !hasLeafNode(from)) {
            throw new IllegalArgumentException("Asked to traverse Network starting at a node not in the network.");
        }
        Map<T, List<T>> visitedNodes = Maps.newHashMap();
        visitedNodes.put(from, Arrays.asList(from));

        Map<T, TraversalParams<T, V>> traversalDataMap = Maps.newHashMap();
        traversalDataMap.put(from, new TraversalParams<T, V>(defaultPredicate, initialValue));

        while (!traversalDataMap.isEmpty()) {
            Map<T, TraversalParams<T, V>> nextTraversalDataMap = Maps.newHashMap();

            for (TraversalDataWithPath<T, V> traversalData : listNextTraversalLevelWithPath(traversalDataMap, visitedNodes)) {
                visitedNodes.put(traversalData.node, traversalData.path);
                TraversalResult<T, U, V> nodeResult = traversal.visitNode(traversalData.node, traversalData.value, traversalData.path);
                if (nodeResult.stopTraversal) {
                    return nodeResult.result;
                }
                Predicate<T> predicate = nodeResult.predicate != null ? nodeResult.predicate : defaultPredicate;
                nextTraversalDataMap.put(traversalData.node, new TraversalParams<T, V>(predicate, nodeResult.value));
            }
            traversalDataMap = nextTraversalDataMap;
        }

        return defaultResult;
    }

    @Override
    public <U, V> U traverseBreadthFirst(T from, BreadthFirstTraversal<T, U, V> traversal, Predicate<T> defaultPredicate,
                                         U defaultResult, V initialValue) {
        if (!hasNetworkingNode(from) && !hasLeafNode(from)) {
            throw new IllegalArgumentException("Asked to traverse Network starting at a node not in the network.");
        }
        return traverseBreadthFirstInternal(from, traversal, defaultPredicate, defaultResult, initialValue);
    }

    private <U, V> U traverseBreadthFirstInternal(NetworkNode from, BreadthFirstTraversal<T, U, V> traversal, Predicate<T> defaultPredicate,
                                                  U defaultResult, V initialValue) {

        Set<NetworkNode> visitedNodes = Sets.newHashSet();
        visitedNodes.add(from);

        Map<NetworkNode, TraversalParams<T, V>> traversalDataMap = Maps.newHashMap();
        traversalDataMap.put(from, new TraversalParams<T, V>(defaultPredicate, initialValue));

        while (!traversalDataMap.isEmpty()) {
            Map<NetworkNode, TraversalParams<T, V>> nextTraversalDataMap = Maps.newHashMap();

            for (TraversalData<T, V> traversalData : listNextTraversalLevel(traversalDataMap, visitedNodes)) {
                visitedNodes.add(traversalData.node);
                TraversalResult<T, U, V> nodeResult = traversal.visitNode(traversalData.node, traversalData.value);

                if (nodeResult.stopTraversal) {
                    return nodeResult.result;
                }
                Predicate<T> predicate = nodeResult.predicate != null ? nodeResult.predicate : defaultPredicate;
                nextTraversalDataMap.put(traversalData.node, new TraversalParams<T, V>(predicate, nodeResult.value));
            }
            traversalDataMap = nextTraversalDataMap;
        }

        return defaultResult;
    }

    private <V> Iterable<TraversalDataWithPath<T, V>> listNextTraversalLevelWithPath(Map<T, TraversalParams<T, V>> traversalDataMap, Map<T, List<T>> visitedNodes) {
        List<TraversalDataWithPath<T, V>> result = Lists.newLinkedList();

        for (Map.Entry<T, TraversalParams<T, V>> nodeEntry : traversalDataMap.entrySet()) {
            T node = nodeEntry.getKey();
            TraversalParams<T, V> params = nodeEntry.getValue();

            for (Side connectingOnSide : SideBitFlag.getSides(node.outputSides)) {
                final ImmutableBlockLocation possibleConnectionLocation = node.location.move(connectingOnSide);
                for (T possibleConnection : networkingNodes.get(possibleConnectionLocation)) {
                    if (!visitedNodes.containsKey(possibleConnection) && SideBitFlag.hasSide(possibleConnection.inputSides, connectingOnSide.reverse())
                            && params.predicate.apply(possibleConnection)) {
                        List<T> path = Lists.newLinkedList(visitedNodes.get(node));
                        path.add(possibleConnection);
                        result.add(new TraversalDataWithPath<T, V>(possibleConnection, params.value, path));
                    }
                }
            }
        }

        return result;
    }

    private <V> Iterable<TraversalData<T, V>> listNextTraversalLevel(Map<NetworkNode, TraversalParams<T, V>> traversalDataMap, Set<NetworkNode> visitedNodes) {
        List<TraversalData<T, V>> result = Lists.newLinkedList();

        for (Map.Entry<NetworkNode, TraversalParams<T, V>> nodeEntry : traversalDataMap.entrySet()) {
            NetworkNode node = nodeEntry.getKey();
            TraversalParams<T, V> params = nodeEntry.getValue();

            for (Side connectingOnSide : SideBitFlag.getSides(node.outputSides)) {
                final ImmutableBlockLocation possibleConnectionLocation = node.location.move(connectingOnSide);
                for (T possibleConnection : networkingNodes.get(possibleConnectionLocation)) {
                    if (!visitedNodes.contains(possibleConnection) && SideBitFlag.hasSide(possibleConnection.inputSides, connectingOnSide.reverse())
                            && params.predicate.apply(possibleConnection)) {
                        result.add(new TraversalData<T, V>(possibleConnection, params.value));
                    }
                }
            }
        }

        return result;
    }

    private static class TraversalData<T, V> {
        private T node;
        private V value;

        private TraversalData(T node, V value) {
            this.node = node;
            this.value = value;
        }
    }

    private static class TraversalDataWithPath<T, V> {
        private T node;
        private V value;
        private List<T> path;

        private TraversalDataWithPath(T node, V value, List<T> path) {
            this.node = node;
            this.value = value;
            this.path = path;
        }
    }

    private static class TraversalParams<T, V> {
        private Predicate<T> predicate;
        private V value;

        private TraversalParams(Predicate<T> predicate, V value) {
            this.predicate = predicate;
            this.value = value;
        }
    }

    /**
     * Represents the result of a node removal from a network.
     *
     * @param <T> The type of nodes in the network being removed from.
     */
    public static class NetworkingNodeRemovalResult<T extends NetworkNode> {
        /**
         * This removal removes the whole network
         */
        public final boolean removesNetwork;
        /**
         * Leaf nodes this removal removes
         */
        public final Set<T> removedLeafNodes;
        /**
         * Set of networks this removal splits the network into
         */
        public final Set<EfficientNetwork<T>> splitResult;

        private NetworkingNodeRemovalResult(boolean removesNetwork, Set<T> removedLeafNodes, Set<EfficientNetwork<T>> splitResult) {
            this.removesNetwork = removesNetwork;
            this.removedLeafNodes = removedLeafNodes;
            this.splitResult = splitResult;
        }

        /**
         * Create a removal result that represents removal of the whole network.
         *
         * @param <T> The type of nodes in the network being removed from.
         * @return A new removal result
         */
        public static <T extends NetworkNode> NetworkingNodeRemovalResult<T> removeNetwork() {
            return new NetworkingNodeRemovalResult<>(true, null, null);
        }

        /**
         * Create a removal result that represents the removal of a given set of leaf nodes.
         *
         * @param removedLeafNodes The leaf nodes that were removed from the network.
         * @param <T> The type of nodes in the network being removed from.
         * @return A new removal result
         */
        public static <T extends NetworkNode> NetworkingNodeRemovalResult<T> removeLeafNodes(Set<T> removedLeafNodes) {
            return new NetworkingNodeRemovalResult<>(false, removedLeafNodes, null);
        }

        /**
         * Create a removal result that represents a removal that splits a network into the given set of networks.
         *
         * @param splitResult The set of networks the removal splits the original network into.
         * @param <T> The type of nodes in the network being removed from.
         * @return A new removal result
         */
        public static <T extends NetworkNode> NetworkingNodeRemovalResult<T> splitNetwork(Set<EfficientNetwork<T>> splitResult) {
            return new NetworkingNodeRemovalResult<>(false, null, splitResult);
        }
    }
}
