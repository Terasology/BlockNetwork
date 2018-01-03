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

import org.terasology.marcinsc.blockFamily.RotationBlockFamily;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.SideBitFlag;
import org.terasology.world.block.Block;
import org.terasology.world.block.family.BlockFamily;

public final class BlockNetworkUtil {
    private BlockNetworkUtil() {
    }

    /**
    * Get the side of a block that is rotated based on the defined side
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
    * @return Rotation data of a block that used by getResultSide, getResultConnections, and getSourceConnections methods {@link #getResultSide, @link #getResultConnections, @link getSourceConnections}
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
    * Give the new side of the block based on its' rotation
    *
    * @param block the block whose rotation is to be checked and whose connection is to be counted
    * @param definedSides the definedSides on which the loop iterates the sides of the block
    * @return a byte of sides
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
    * Give the new reversed side of the block based on its' rotation
    *
    * @param block the block to be checked
    * @param connections the connections from which the sides come from
    * @return a byte of sides
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
