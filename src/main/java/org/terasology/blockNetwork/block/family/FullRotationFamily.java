// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.blockNetwork.block.family;

import com.google.common.collect.Maps;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockBuilderHelper;
import org.terasology.engine.world.block.BlockUri;
import org.terasology.engine.world.block.family.AbstractBlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.family.RegisterBlockFamily;
import org.terasology.engine.world.block.loader.BlockFamilyDefinition;
import org.terasology.gestalt.naming.Name;

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
