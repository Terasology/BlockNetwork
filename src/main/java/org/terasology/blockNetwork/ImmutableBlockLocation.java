// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import org.terasology.engine.math.Side;
import org.terasology.math.geom.Vector3i;

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

    public ImmutableBlockLocation(Vector3i location) {
        this(location.x, location.y, location.z);
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
     * @return a new ImmutableBlockLocation determined by adding the x-component of the directionVector to x, the
     *         y-component of the directionVector to y, and the z-component of the directionVector to z.
     */
    public ImmutableBlockLocation move(Side side) {
        final Vector3i directionVector = side.getVector3i();
        return new ImmutableBlockLocation(x + directionVector.x, y + directionVector.y, z + directionVector.z);
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
     * @return <code>true</code> if this is equal to object o. Will also return <code>true</code> if o exists and has
     *         the same
     *         x-y-z coordinates as this.
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
        return z == that.z;
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
