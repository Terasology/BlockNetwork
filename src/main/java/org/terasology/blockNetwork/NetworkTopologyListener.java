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

import java.util.Set;

public interface NetworkTopologyListener {
    void networkAdded(Network network);

    void networkingNodesAdded(Network network, Set<NetworkNode> networkingNodes);

    void networkingNodesRemoved(Network network, Set<NetworkNode> networkingNodes);

    void leafNodesAdded(Network network, Set<NetworkNode> leafNodes);

    void leafNodesRemoved(Network network, Set<NetworkNode> leafNodes);

    void networkRemoved(Network network);
}
