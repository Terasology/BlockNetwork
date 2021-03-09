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

import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.engine.math.Side;

/**
 * Contains a block location immutably.
 */
public class ImmutableBlockLocation {
    /**
     * x-coordinate of the location.
     */
    public final int x;

    /**
     * y-coordinate of the location.
     */
    public final int y;

    /**
     * z-coordinate of the location.
     */
    public final int z;

    public ImmutableBlockLocation(Vector3ic location) {
        this(location.x(), location.y(), location.z());
    }

     /**
     * ImmutableBlockLocation constructor.
     *
     * @param x x-coordinate
     * @param y y-coordinate
     * @param z z-coordinate
     */
    public ImmutableBlockLocation(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

     /**
     * Creates a new ImmutableBlockLocation in a relative location.
     *
     * @param side a specific side.
     * @return a new ImmutableBlockLocation determined by adding the x-component of the directionVector to x, the y-component
     * of the directionVector to y, and the z-component of the directionVector to z.
     */
    public ImmutableBlockLocation move(Side side) {
        final Vector3ic directionVector = side.direction();
        return new ImmutableBlockLocation(x + directionVector.x(), y + directionVector.y(), z + directionVector.z());
    }

     /**
     * Creates a Vector3i.
     *
     * @return a new Vector3i with the same x-y-z coordinates of this.
     */
    public Vector3i toVector3i() {
        return new Vector3i(x, y, z);
    }

     /**
     * Reports if two objects are equal to each other.
     *
     * @param o An object.
     * @return <code>true</code> if this is equal to object o. Will also return <code>true</code> if o exists and has the same
     * x-y-z coordinates as this.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ImmutableBlockLocation that = (ImmutableBlockLocation) o;

        if (x != that.x) {
            return false;
        }
        if (y != that.y) {
            return false;
        }
        if (z != that.z) {
            return false;
        }

        return true;
    }

    /**
     * Reports the hash code of a block's location.
     *
     * @return result The hash code of a block's location based on the x-y-z coordinates.
     */
    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }
}
