// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork.block.family;

import org.terasology.engine.math.Rotation;
import org.terasology.engine.world.block.Block;

/**
 * Block family interface that defined its blocks by rotation.
 */
public interface RotationBlockFamily {
    /**
     * Returns block from the block family for the specified rotation.
     *
     * @param rotation
     * @return
     */
    Block getBlockForRotation(Rotation rotation);

    /**
     * Returns rotation used to create the specified block.
     *
     * @param block
     * @return
     */
    Rotation getRotation(Block block);
}
