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

import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.math.geom.Vector3i;

/**
 * Represents a single node in a block network.
 * Any instance of this class or its children can be used as a node in {@link EfficientNetwork}
 */
public class NetworkNode {
    /**
     * The location of the Node
     */
    public final ImmutableBlockLocation location;
    
    /**
     * Based on a combination of the input and output sides, i.e. any side that is an input, output, or both.
     */
    public final byte connectionSides;
    
    /**
     * Sides which can be used for input
     */
    public final byte inputSides;
    
    /**
     * Sides which can be used for output
     */
    public final byte outputSides;

    /**
     * @deprecated Use the constructor with separate input and output sides.
     * @param location
     * @param connectionSides
     */
    @Deprecated
    public NetworkNode(Vector3i location, byte connectionSides) {
        this(location, connectionSides, connectionSides);
    }

    /**
     * @deprecated Use the constructor with separate input and output sides.
     * @param location
     * @param sides
     */
    @Deprecated
    public NetworkNode(Vector3i location, Side... sides) {
        this(location, SideBitFlag.getSides(sides));
    }

    /**
     * Creates a new node based on the given location and input/output sides.
     * @param location The location of the node
     * @param inputSides The sides which can be used for input
     * @param outputSides The sides which can be used for output
     * @throws IllegalArgumentException if the input or output sides don't represent the sides of a block (i.e., they are outside of the 0-63 range)
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
     * Used for finding out whether or not two NetworkNodes are the same
     * @param o The node being compared
     * @return true only if the node being compared to has the same inputSides, outputSides, and location
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NetworkNode that = (NetworkNode) o;

        if (inputSides != that.inputSides) return false;
        if (outputSides != that.outputSides) return false;
        if (location != null ? !location.equals(that.location) : that.location != null) return false;

        return true;
    }

    /**
     * @return A unique number for every different NetworkNode
     */
    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (int) inputSides;
        result = 31 * result + (int) outputSides;
        return result;
    }

    /**
     * @return A String with the location as an ordered triple and connection sides in base 10
     */
    @Override
    public String toString() {
        return location.toVector3i().toString() + " " + connectionSides;
    }
}
