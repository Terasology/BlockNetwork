// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents one network of nodes, where each node is somehow connected to another within the network.
 * <p>
 * Network contains following node types: - networking nodes - nodes that are a back-bone of a network. These allow to
 * connect multiple nodes in the network. A networking node "conducts" the "signal" of the network to nodes defined in
 * the "connectingOnSides" nodes in its vicinity. - leaf nodes - nodes that are only receiving or producing a signal,
 * and do not themselves "conduct" it to other nodes.
 * <p>
 * A couple of non-obvious facts: 1. The same node (defined as location) cannot be both a networking node and a leaf
 * node in the same network. 2. The same leaf node can be a member of multiple disjunctive networks (different network
 * on each side). 3. A valid network can have no networking nodes at all, and exactly two leaf nodes (neighbouring leaf
 * nodes).
 *
 * @deprecated Use EfficientNetwork, this class will be removed.
 */
@Deprecated
public class SimpleNetwork<T extends NetworkNode> implements Network<T> {
    private static final boolean SANITY_CHECK = false;
    private final SetMultimap<ImmutableBlockLocation, T> networkingNodes = HashMultimap.create();
    private final SetMultimap<ImmutableBlockLocation, T> leafNodes = HashMultimap.create();

    // Distance cache
    private final Map<TwoNetworkNodes, Integer> distanceCache = Maps.newHashMap();
    private final Map<TwoNetworkNodes, List<T>> shortestRouteCache = Maps.newHashMap();

    public static <T extends NetworkNode> SimpleNetwork<T> createDegenerateNetwork(
            T networkNode1,
            T networkNode2) {
        if (!areNodesConnecting(networkNode1, networkNode2)) {
            throw new IllegalArgumentException("These two nodes are not connected");
        }

        SimpleNetwork<T> network = new SimpleNetwork<T>();
        network.leafNodes.put(networkNode1.location, networkNode1);
        network.leafNodes.put(networkNode2.location, networkNode2);
        return network;
    }

    public static boolean areNodesConnecting(NetworkNode node1, NetworkNode node2) {
        for (Side side : SideBitFlag.getSides(node1.connectionSides)) {
            final ImmutableBlockLocation possibleConnectedLocation = node1.location.move(side);
            if (node2.location.equals(possibleConnectedLocation) && SideBitFlag.hasSide(node2.connectionSides,
                    side.reverse())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Adds a networking node to the network.
     *
     * @param networkNode Definition of the networking node position and connecting sides.
     */
    public void addNetworkingNode(T networkNode) {
        if (SANITY_CHECK && !canAddNetworkingNode(networkNode)) {
            throw new IllegalStateException("Unable to add this node to network");
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
            throw new IllegalStateException("Unable to add this node to network");
        }
        leafNodes.put(networkNode.location, networkNode);
        distanceCache.clear();
        shortestRouteCache.clear();
    }

    /**
     * Returns the network size - a number of nodes it spans. If the same node is added twice with different connecting
     * sides, it is counted twice.
     *
     * @return The sum of networking nodes and leaf nodes (count).
     */
    @Override
    public int getNetworkSize() {
        return networkingNodes.size() + leafNodes.size();
    }

    /**
     * Removes a leaf node from the network. If this removal made the network degenerate, it will return
     * <code>true</code>.
     *
     * @param networkingNode Definition of the leaf node position and connecting sides.
     * @return <code>true</code> if the network after the removal is degenerated or empty (no longer valid).
     */
    public boolean removeLeafNode(T networkingNode) {
        // Removal of a leaf node cannot split the network, so it's just safe to remove it
        // We just need to check, if after removal of the node, network becomes degenerated, if so - we need
        // to signal that the network is no longer valid and should be removed.
        final boolean changed = leafNodes.remove(networkingNode.location, networkingNode);
        if (!changed) {
            throw new IllegalStateException("Tried to remove a node that is not in the network");
        }

        distanceCache.clear();
        shortestRouteCache.clear();

        return isDegeneratedNetwork() || isEmptyNetwork();
    }

    public void removeAllLeafNodes() {
        leafNodes.clear();
        distanceCache.clear();
        shortestRouteCache.clear();
    }

    public void removeAllNetworkingNodes() {
        networkingNodes.clear();
        distanceCache.clear();
        shortestRouteCache.clear();
    }

    public void removeNetworkingNode(T networkNode) {
        if (!networkingNodes.remove(networkNode.location, networkNode)) {
            throw new IllegalStateException("Tried to remove a node that is not in the network");
        }
        distanceCache.clear();
        shortestRouteCache.clear();
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
        for (Side connectingOnSide : SideBitFlag.getSides(networkNode.connectionSides)) {
            final ImmutableBlockLocation possibleConnectionLocation = networkNode.location.move(connectingOnSide);
            for (NetworkNode possibleConnectedNode : networkingNodes.get(possibleConnectionLocation)) {
                if (SideBitFlag.hasSide(possibleConnectedNode.connectionSides, connectingOnSide.reverse())) {
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

        if (SimpleNetwork.areNodesConnecting(from, to)) {
            return 1;
        }

        // Breadth-first search of the network
        Set<NetworkNode> visitedNodes = Sets.newHashSet();
        visitedNodes.add(from);

        Set<NetworkNode> networkingNodesToTest = Sets.newHashSet();
        listConnectedNotVisitedNetworkingNodes(visitedNodes, from, networkingNodesToTest);
        int distanceSearched = 1;
        while (networkingNodesToTest.size() > 0) {
            distanceSearched++;
            if (distanceSearched > maxToSearch) {
                return -1;
            }

            for (NetworkNode nodeToTest : networkingNodesToTest) {
                if (SimpleNetwork.areNodesConnecting(nodeToTest, to)) {
                    distanceCache.put(
                            new TwoNetworkNodes(from, to),
                            distanceSearched);
                    return distanceSearched;
                }
                visitedNodes.add(nodeToTest);
            }

            Set<NetworkNode> nextNetworkingNodesToTest = Sets.newHashSet();
            for (NetworkNode nodeToTest : networkingNodesToTest) {
                listConnectedNotVisitedNetworkingNodes(visitedNodes, nodeToTest, nextNetworkingNodesToTest);
            }

            networkingNodesToTest = nextNetworkingNodesToTest;
        }
        return -1;
    }

    @Override
    public int getDistanceWithSide(T from, T to, Side toSide, int maxToSearch) {
        validateAnyOfTheNodesInNetwork(from, to);

        NetworkNode destination = new NetworkNode(to.location.toVector3i(), toSide);
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

        if (SimpleNetwork.areNodesConnecting(from, to)) {
            return Arrays.asList(from, to);
        }

        // Breadth-first search of the network
        Map<T, List<T>> visitedNodes = Maps.newHashMap();
        visitedNodes.put(from, Arrays.asList(from));

        Map<T, List<T>> networkingNodesToTest = Maps.newHashMap();
        listConnectedNotVisitedNetworkingNodes(visitedNodes, from, networkingNodesToTest);
        while (networkingNodesToTest.size() > 0) {
            for (Map.Entry<T, List<T>> nodeToTest : networkingNodesToTest.entrySet()) {
                if (SimpleNetwork.areNodesConnecting(nodeToTest.getKey(), to)) {
                    List<T> route = new LinkedList<>(nodeToTest.getValue());
                    route.add(to);

                    shortestRouteCache.put(
                            new TwoNetworkNodes(from, to),
                            route);
                    return Collections.unmodifiableList(route);
                }
                visitedNodes.put(nodeToTest.getKey(), nodeToTest.getValue());
            }

            Map<T, List<T>> nextNetworkingNodesToTest = Maps.newHashMap();
            for (Map.Entry<T, List<T>> nodeToTest : networkingNodesToTest.entrySet()) {
                listConnectedNotVisitedNetworkingNodes(visitedNodes, nodeToTest.getKey(), nextNetworkingNodesToTest);
            }

            networkingNodesToTest = nextNetworkingNodesToTest;
        }
        return null;
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
            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.connectionSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : leafNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.connectionSides, connectingOnSide.reverse())) {
                        return SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }

            return 0;
        } else {
            byte result = 0;
            for (Side connectingOnSide : SideBitFlag.getSides(networkNode.connectionSides)) {
                Vector3i possibleLocation = networkNode.location.toVector3i();
                possibleLocation.add(connectingOnSide.getVector3i());
                for (NetworkNode node : networkingNodes.get(new ImmutableBlockLocation(possibleLocation))) {
                    if (SideBitFlag.hasSide(node.connectionSides, connectingOnSide.reverse())) {
                        result += SideBitFlag.getSide(connectingOnSide);
                    }
                }
            }

            return result;
        }
    }

    private void listConnectedNotVisitedNetworkingNodes(Set<NetworkNode> visitedNodes, NetworkNode location,
                                                        Collection<NetworkNode> result) {
        for (Side connectingOnSide : SideBitFlag.getSides(location.connectionSides)) {
            final ImmutableBlockLocation possibleConnectionLocation = location.location.move(connectingOnSide);
            for (T possibleConnection : networkingNodes.get(possibleConnectionLocation)) {
                if (!visitedNodes.contains(possibleConnection) && SideBitFlag.hasSide(possibleConnection.connectionSides, connectingOnSide.reverse())) {
                    result.add(possibleConnection);
                }
            }
        }
    }

    private void listConnectedNotVisitedNetworkingNodes(Map<T, List<T>> visitedNodes, T location,
                                                        Map<T, List<T>> result) {
        for (Side connectingOnSide : SideBitFlag.getSides(location.connectionSides)) {
            final ImmutableBlockLocation possibleConnectionLocation = location.location.move(connectingOnSide);
            for (T possibleConnection : networkingNodes.get(possibleConnectionLocation)) {
                if (!visitedNodes.containsKey(possibleConnection) && SideBitFlag.hasSide(possibleConnection.connectionSides, connectingOnSide.reverse())) {
                    List<T> pathToNewNode = new LinkedList<>(visitedNodes.get(location));
                    pathToNewNode.add(possibleConnection);
                    result.put(possibleConnection, pathToNewNode);
                }
            }
        }
    }

    private boolean isDegeneratedNetwork() {
        return networkingNodes.isEmpty() && leafNodes.size() == 1;
    }

    private boolean isEmptyNetwork() {
        return networkingNodes.isEmpty() && leafNodes.isEmpty();
    }
}
