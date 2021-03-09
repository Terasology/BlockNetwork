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

import org.terasology.engine.math.Side;

import java.util.Collection;
import java.util.List;

/**
 * Contains basic functionality for a network.
 *
 * @param <T> The type of a node in this network.
 */
public interface Network<T extends NetworkNode> {
    /**
     * Checks if a given node is a networking node in this network.
     *
     * @param networkNode The node to check for.
     * @return true if <code>networkNode</code> is a networking node of this network, false otherwise.
     */
    boolean hasNetworkingNode(T networkNode);

    /**
     * Checks if a given node is a leaf node in this network.
     *
     * @param networkNode The node to check for.
     * @return true if <code>networkNode</code> is a leaf node of this network, false otherwise.
     */
    boolean hasLeafNode(T networkNode);

    /**
     * The number of nodes in this network.
     *
     * @return The number of nodes in this network, as an integer.
     */
    int getNetworkSize();

    /**
     * Gets the distance between two given nodes.
     *
     * @param from The node to start from.
     * @param to The node to measure to.
     * @param maxToSearch The maximum distance to search before giving up.
     * @return The distance between <code>from</code> and <code>to</code> if its less than or equal to <code>maxToSearch</code>, otherwise -1.
     */
    int getDistance(T from, T to, int maxToSearch);

    /**
     * Gets the distance between a given node and a given destination node through a given destination side.
     *
     * @param from The node to start from.
     * @param to The node to measure to.
     * @param toSide The target side of <code>to</code> to measure through.
     * @param maxToSearch The maximum distance to search before giving up.
     * @return The distance between <code>from</code> and <code>to</code> through <code>toSide</code> if its less than or equal to <code>maxToSearch</code>, otherwise -1.
     */
    int getDistanceWithSide(T from, T to, Side toSide, int maxToSearch);

    /**
     * Finds the shortest route between two given nodes.
     *
     * @param from The node to start from.
     * @param to The node to go to.
     * @return A list that start with <code>from</code>, contains the nodes the shortest route traverses in order, and ends with <code>to</code>.
     */
    List<T> findShortestRoute(T from, T to);

    /**
     * Tests a given leaf node for matching inputs/outputs with this network.
     *
     * @param networkNode The node to test.
     * @return A bit field of sides to which <code>networkNode</code> connect to this network, which are either outputs connecting to a node with an input, or inputs connecting to a node with an output.
     */
    byte getLeafSidesInNetwork(T networkNode);

    /**
     * Gets the networking nodes in this network.
     *
     * @return A collection of networking nodes.
     */
    Collection<T> getNetworkingNodes();

    /**
     * Gets the leaf nodes in this network.
     *
     * @return A collection of leaf nodes.
     */
    Collection<T> getLeafNodes();
}
