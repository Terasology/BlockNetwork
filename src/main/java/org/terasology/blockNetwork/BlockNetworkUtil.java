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
    * getResultSide
    * @param block - the block
    * @param definedSide - the definedSide
    * @return a side rotation of the definedSide
    * This method's function is to get the side of a block that is rotated somehow. The rotation of the block
    * is obtained from the getRotation Method ({@link #getRotation}) and then to get the Side data, the rotation data
    * is rotated to the defined side
    */
    public static Side getResultSide(Block block, Side definedSide) {
        Rotation rotation = getRotation(block);
        return rotation.rotate(definedSide);
    }

    /**
    * getRotation
    * @param block - the block
    * @return Rotation data of a block that used by getResultSide, getResultConnections, and getSourceConnections methods {@link #getResultSide, @link #getResultConnections, @link getSourceConnections}
    * This Rotation method refers to the RotationBlockFamily class to get the rotation of a block. If the blockfamily of the block has RotationBlockFamily in instance,
    * The method will return the rotation of the block. if not, it will return null
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
    * getResultConnections
    * @param block - the block
    * @param definedSides - the definedSides
    * @return a byte contains the amount of result connections of the block
    * This method is used to count the result connection of the block. the iteration of rotating the block in definedSides will increase the amount
    * of the connections
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
    * getSourceConnections
    * @param block - the block
    * @param connections - the connections
    * @return a byte contains the amount of source connections of the block
    * This getSourceConnections method works almost similar to the getResultConnections method above. but, there's a different in the rotation var.
    * The rotation var in this method finds the reverse of the block rotation of a defined block. There is also a different at the loop in this method.
    * This method loops the sides of the connections defined as a parameter while the getResultConnections iterates from the definedSides.
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
