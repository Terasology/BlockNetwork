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
import org.terasology.math.geom.ImmutableVector3i;
import org.terasology.math.geom.Vector3i;

public class NetworkNode {
    public final ImmutableVector3i location;
    public final byte connectionSides;
    public final byte inputSides;
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

    public NetworkNode(Vector3i location, byte inputSides, byte outputSides) {
        if (inputSides > 63 || inputSides < 0 || outputSides > 63 || outputSides < 0) {
            throw new IllegalArgumentException("Connection sides has to be in the 0-63 range");
        }
        this.location = new ImmutableVector3i(location.x, location.y, location.z);
        this.connectionSides = (byte) (inputSides | outputSides);
        this.inputSides = inputSides;
        this.outputSides = outputSides;
    }

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

    @Override
    public int hashCode() {
        int result = location != null ? location.hashCode() : 0;
        result = 31 * result + (int) inputSides;
        result = 31 * result + (int) outputSides;
        return result;
    }

    @Override
    public String toString() {
        return location.toString() + " " + connectionSides;
    }
}
