// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import java.util.Set;

/**
 * @param <T>
 * @deprecated Use EfficientBlockNetwork instead with EfficientNetworkTopologyListener, this class will be removed.
 */
@Deprecated
public interface NetworkTopologyListener<T extends NetworkNode> {
    void networkAdded(Network<T> network);

    void networkingNodesAdded(Network<T> network, Set<T> networkingNodes);

    void networkingNodesRemoved(Network<T> network, Set<T> networkingNodes);

    void leafNodesAdded(Network<T> network, Set<T> leafNodes);

    void leafNodesRemoved(Network<T> network, Set<T> leafNodes);

    void networkRemoved(Network<T> network);
}
