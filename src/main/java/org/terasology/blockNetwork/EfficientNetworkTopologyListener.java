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

import java.util.Set;

public interface EfficientNetworkTopologyListener<T extends NetworkNode> {
    /**
     * Called when network was added (with all the nodes within it).
     * @param network
     * @param reason
     */
    default void networkAdded(Network2<T> network, NetworkChangeReason reason) { }

    /**
     * Called when a network was split into multiple networks.
     * @param oldNetwork
     * @param resultNetworks
     * @param reason
     */
    default void networkSplit(Network2<T> oldNetwork, Set<? extends Network2<T>> resultNetworks, NetworkChangeReason reason) {
        networkRemoved(oldNetwork, reason);
        for (Network2<T> resultNetwork : resultNetworks) {
            networkAdded(resultNetwork, reason);
        }
    }

    /**
     * Called when multiple networks were merged into one network.
     * @param oldNetworks
     * @param newNetwork
     * @param reason
     */
    default void networksMerged(Set<? extends Network2<T>> oldNetworks, Network2<T> newNetwork, NetworkChangeReason reason) {
        for (Network2<T> oldNetwork : oldNetworks) {
            networkRemoved(oldNetwork, reason);
        }
        networkAdded(newNetwork, reason);
    }

    /**
     * Called when network was removed (with all the nodes within it).
     * @param network
     * @param reason
     */
    default void networkRemoved(Network2<T> network, NetworkChangeReason reason) { }

    /**
     * Called when networking nodes were added to an existing network.
     * @param network
     * @param networkingNodes
     * @param reason
     */
    default void networkingNodesAdded(Network2<T> network, Set<T> networkingNodes, NetworkChangeReason reason) { }

    /**
     * Called when networking nodes were removed from an existing network.
     * @param network
     * @param networkingNodes
     * @param reason
     */
    default void networkingNodesRemoved(Network2<T> network, Set<T> networkingNodes, NetworkChangeReason reason) { }

    /**
     * Called when leaf nodes were added to an existing network.
     * @param network
     * @param leafNodes
     * @param reason
     */
    default void leafNodesAdded(Network2<T> network, Set<T> leafNodes, NetworkChangeReason reason) { }

    /**
     * Called when leaf nodes were removed from an existing network.
     * @param network
     * @param leafNodes
     * @param reason
     */
    default void leafNodesRemoved(Network2<T> network, Set<T> leafNodes, NetworkChangeReason reason) { }
}
