// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import java.util.Set;

/**
 * Listener class for topology changes in an {@link EfficientNetwork}.
 *
 * @param <T> The type of node in the network listened to.
 */
public interface EfficientNetworkTopologyListener<T extends NetworkNode> {
    /**
     * Called when network was added (with all the nodes within it).
     *
     * @param network
     * @param reason
     */
    default void networkAdded(Network2<T> network, NetworkChangeReason reason) {
    }

    /**
     * Called when a network was split into multiple networks.
     *
     * @param oldNetwork
     * @param resultNetworks
     * @param reason
     */
    default void networkSplit(Network2<T> oldNetwork, Set<? extends Network2<T>> resultNetworks,
                              NetworkChangeReason reason) {
        networkRemoved(oldNetwork, reason);
        for (Network2<T> resultNetwork : resultNetworks) {
            networkAdded(resultNetwork, reason);
        }
    }

    /**
     * Called when multiple networks were merged into one network.
     *
     * @param oldNetworks
     * @param newNetwork
     * @param reason
     */
    default void networksMerged(Set<? extends Network2<T>> oldNetworks, Network2<T> newNetwork,
                                NetworkChangeReason reason) {
        for (Network2<T> oldNetwork : oldNetworks) {
            networkRemoved(oldNetwork, reason);
        }
        networkAdded(newNetwork, reason);
    }

    /**
     * Called when network was removed (with all the nodes within it).
     *
     * @param network
     * @param reason
     */
    default void networkRemoved(Network2<T> network, NetworkChangeReason reason) {
    }

    /**
     * Called when networking nodes were added to an existing network.
     *
     * @param network
     * @param networkingNodes
     * @param reason
     */
    default void networkingNodesAdded(Network2<T> network, Set<T> networkingNodes, NetworkChangeReason reason) {
    }

    /**
     * Called when networking nodes were removed from an existing network.
     *
     * @param network
     * @param networkingNodes
     * @param reason
     */
    default void networkingNodesRemoved(Network2<T> network, Set<T> networkingNodes, NetworkChangeReason reason) {
    }

    /**
     * Called when leaf nodes were added to an existing network.
     *
     * @param network
     * @param leafNodes
     * @param reason
     */
    default void leafNodesAdded(Network2<T> network, Set<T> leafNodes, NetworkChangeReason reason) {
    }

    /**
     * Called when leaf nodes were removed from an existing network.
     *
     * @param network
     * @param leafNodes
     * @param reason
     */
    default void leafNodesRemoved(Network2<T> network, Set<T> leafNodes, NetworkChangeReason reason) {
    }
}
