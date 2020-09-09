// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

/**
 * Represents a single node in a block network. Any instance of this class or its children can be used as a node in
 * {@link EfficientNetwork}
 */
public class NetworkNode {
    /**
     * The location this node represents.
     */
    public final ImmutableBlockLocation location;
    //This variable is created based on a combination of the input and output sides, i.e. it contains any side that
    // is an input, output, or both.
    /**
     * A bit field of all sides this node connects on.
     *
     * @see SideBitFlag
     */
    public final byte connectionSides;

    /**
     * A bit field of all sides from which this node has inputs.
     *
     * @see SideBitFlag
     */
    public final byte inputSides;

    /**
     * A bit field of all sides from which this node has outputs.
     *
     * @see SideBitFlag
     */
    public final byte outputSides;

    /**
     * @param location
     * @param connectionSides
     * @deprecated Use the constructor with separate input and output sides.
     */
    @Deprecated
    public NetworkNode(Vector3i location, byte connectionSides) {
        this(location, connectionSides, connectionSides);
    }

    /**
     * @param location
     * @param sides
     * @deprecated Use the constructor with separate input and output sides.
     */
    @Deprecated
    public NetworkNode(Vector3i location, Side... sides) {
        this(location, SideBitFlag.getSides(sides));
    }

    /**
     * Creates a new node based on the given location and input/output sides.
     *
     * @param location The location of the node
     * @param inputSides The sides which can be used for input
     * @param outputSides The sides which can be used for output
     * @throws IllegalArgumentException if the input or output sides don't represent the sides of a block (i.e.,
     *         they are outside of the 0-63 range)
     */
    public NetworkNode(Vector3i location, byte inputSides, byte outputSides) {
        if (inputSides > 63 || inputSides < 0 || outputSides > 63 || outputSides < 0) {
            throw new IllegalArgumentException("Connection sides has to be in the 0-63 range");
        }
        this.location = new ImmutableBlockLocation(location.x, location.y, location.z);
        this.connectionSides = (byte) (inputSides | outputSides);
        this.inputSides = inputSides;
        this.outputSides = outputSides;
    }

    /**
     * {@inheritDoc} Note that two Nodes are are considered equal only if the location, inputSides, and outputSides are
     * the same
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkNode that = (NetworkNode) o;

        if (inputSides != that.inputSides) return false;
        if (outputSides != that.outputSides) return false;
        return location != null ? location.equals(that.location) : that.location == null;
    }

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (int) inputSides;
        result = 31 * result + (int) outputSides;
        return result;
    }

    @Override
    public String toString() {
        return location.toVector3i().toString() + " " + connectionSides;
    }
}
