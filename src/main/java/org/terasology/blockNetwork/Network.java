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
