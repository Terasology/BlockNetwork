// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork;

import org.terasology.blockNetwork.block.family.RotationBlockFamily;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SideBitFlag;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.family.BlockFamily;

public final class BlockNetworkUtil {
    private BlockNetworkUtil() {
    }

    /**
     * Gets the side of a block that is rotated based on the defined side
     *
     * @param block the block that is rotated
     * @param definedSide the definedSide to define which side of the rotated block to be returned
     * @return a side rotation of the definedSide
     */
    public static Side getResultSide(Block block, Side definedSide) {
        Rotation rotation = getRotation(block);
        return rotation.rotate(definedSide);
    }

    /**
     * Check if the block has RotationBlockFamily and return the rotation of the block if it does
     *
     * @param block the block whose rotation is to be checked
     * @return Rotation data of a block that used by getResultSide, getResultConnections, and getSourceConnections
     *         methods {@link #getResultSide, @link #getResultConnections, @link getSourceConnections}
     */
    private static Rotation getRotation(Block block) {
        Rotation rotation = Rotation.none();
        BlockFamily blockFamily = block.getBlockFamily();
        if (blockFamily instanceof RotationBlockFamily) {
            rotation = ((RotationBlockFamily) blockFamily).getRotation(block);
        }
        return rotation;
    }

    /**
     * Gives the new sides of the block based on its rotation
     *
     * @param block the block whose rotation is to be checked and whose connection is to be counted
     * @param definedSides a byte of sides that are to be added to the result byte
     * @return a byte containing the new sides
     */
    public static byte getResultConnections(Block block, byte definedSides) {
        Rotation rotation = getRotation(block);

        byte result = 0;
        for (Side side : SideBitFlag.getSides(definedSides)) {
            result = SideBitFlag.addSide(result, rotation.rotate(side));
        }

        return result;
    }

    /**
     * Gives the new reversed sides of the block based on its rotation
     *
     * @param block the block to be checked
     * @param connections a byte of connections that is to be added to the result byte
     * @return a byte of new sides
     */
    public static byte getSourceConnections(Block block, byte connections) {
        Rotation rotation = getRotation(block);

        rotation = Rotation.findReverse(rotation);

        byte result = 0;
        for (Side side : SideBitFlag.getSides(connections)) {
            result = SideBitFlag.addSide(result, rotation.rotate(side));
        }

        return result;
    }
}
