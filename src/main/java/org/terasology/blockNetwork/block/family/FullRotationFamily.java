/*
 * Copyright 2020 MovingBlocks
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
package org.terasology.blockNetwork.block.family;

import com.google.common.collect.Maps;
import org.terasology.math.Rotation;
import org.terasology.math.Side;
import org.terasology.math.geom.Vector3i;
import org.terasology.naming.Name;
import org.terasology.world.block.Block;
import org.terasology.world.block.BlockBuilderHelper;
import org.terasology.world.block.BlockUri;
import org.terasology.world.block.family.AbstractBlockFamily;
import org.terasology.world.block.family.BlockPlacementData;
import org.terasology.world.block.family.RegisterBlockFamily;
import org.terasology.world.block.loader.BlockFamilyDefinition;

import java.util.Map;

@RegisterBlockFamily("fullRotation")
public class FullRotationFamily extends AbstractBlockFamily implements RotationBlockFamily {
    private Map<Rotation, Block> blocks = Maps.newHashMap();
    private Map<BlockUri, Block> blockUriMap = Maps.newHashMap();
    private Block archetypeBlock;

    public FullRotationFamily(BlockFamilyDefinition definition, BlockBuilderHelper helper) {
        super(definition, helper);
        super.setCategory(definition.getCategories());

        BlockUri familyUri = new BlockUri(definition.getUrn());
        super.setBlockUri(familyUri);

        for (Rotation rot : Rotation.values()) {
            BlockUri blockUri = new BlockUri(familyUri, new Name(rot.getYaw().ordinal() + "." + rot.getPitch().ordinal() + "." + rot.getRoll().ordinal()));
            Block block = helper.constructTransformedBlock(definition, rot, blockUri, this);
            block.setUri(blockUri);

            blocks.put(rot, block);
            blockUriMap.put(blockUri, block);
        }

        archetypeBlock = blocks.get(Rotation.none());
    }

    @Override
    public Block getBlockForPlacement(BlockPlacementData data) {
        return getBlockForPlacement(data.attachmentSide);
    }

    @Override
    public Block getBlockForPlacement(Vector3i location, Side attachmentSide, Side direction) {
        return getBlockForPlacement(attachmentSide);
    }

    private Block getBlockForPlacement(Side attachmentSide) {
        // Find first one so that FRONT Side of the original block is same as attachmentSide
        for (Map.Entry<Rotation, Block> rotationBlockEntry : blocks.entrySet()) {
            if (rotationBlockEntry.getKey().rotate(Side.FRONT) == attachmentSide) {
                return rotationBlockEntry.getValue();
            }
        }
        return null;
    }

    @Override
    public Block getArchetypeBlock() {
        return archetypeBlock;
    }

    @Override
    public Block getBlockFor(BlockUri blockUri) {
        return blockUriMap.get(blockUri);
    }

    @Override
    public Iterable<Block> getBlocks() {
        return blocks.values();
    }

    @Override
    public Block getBlockForRotation(Rotation rotation) {
        return blocks.get(rotation);
    }

    @Override
    public Rotation getRotation(Block block) {
        return findRotationForBlock(block);
    }

    private Rotation findRotationForBlock(Block block) {
        for (Map.Entry<Rotation, Block> rotationBlockEntry : blocks.entrySet()) {
            if (rotationBlockEntry.getValue() == block) {
                return rotationBlockEntry.getKey();
            }
        }
        return null;
    }
}
