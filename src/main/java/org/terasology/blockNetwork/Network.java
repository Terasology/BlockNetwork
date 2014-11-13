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

import org.terasology.math.Side;

import java.util.Collection;
import java.util.List;

public interface Network {
    boolean hasNetworkingNode(NetworkNode networkNode);

    boolean hasLeafNode(NetworkNode networkNode);

    int getNetworkSize();

    int getDistance(NetworkNode from, NetworkNode to);

    List<NetworkNode> findShortestRoute(NetworkNode from, NetworkNode to);

    boolean isInDistance(int distance, NetworkNode from, NetworkNode to);

    byte getLeafSidesInNetwork(NetworkNode networkNode);

    boolean isInDistanceWithSide(int distance, NetworkNode from, NetworkNode to, Side toSide);

    Collection<NetworkNode> getNetworkingNodes();

    Collection<NetworkNode> getLeafNodes();
}
