package com.unrealdinnerbone.javd.util;

import com.unrealdinnerbone.javd.JAVD;
import com.unrealdinnerbone.javd.JAVDRegistry;
import com.unrealdinnerbone.javd.block.PortalBlock;
import com.unrealdinnerbone.javd.block.PortalTileEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class TelerportUtils {


    public static void teleport(PlayerEntity playerEntity, World toWorld, BlockPos blockPos, boolean spawnPlatform) throws RuntimeException {
        if(!toWorld.isRemote() && playerEntity.world instanceof ServerWorld && toWorld instanceof ServerWorld) {
            BlockPos portalLocation = findPortalLocation(playerEntity.getEntityWorld(), blockPos).orElseThrow(() -> new RuntimeException("Cant find location to spawn portal"));
            if (toWorld.getBlockState(portalLocation).isAir()) {
                Block block = ListUtil.getRandom(JAVD.GENERATOR_BLOCKS.getAllElements()).orElse(Blocks.STONE);
                if(spawnPlatform && toWorld.getBlockState(portalLocation).isAir()) {
                    int range = JAVD.PLATFORM_RANGE.get();
                    BlockPos.getAllInBox(portalLocation.add(range, 0, range), portalLocation.add(-range, 0, -range)).forEach(blockPos1 -> {
                        if (toWorld.getBlockState(blockPos1).isAir()) {
                            toWorld.setBlockState(blockPos1, block.getDefaultState());
                        }
                    });
                }
                PortalBlock.placeBlock(toWorld, portalLocation, playerEntity.world.getDimensionKey());
            }
            playerEntity.changeDimension((ServerWorld) toWorld, new SimpleTeleporter(portalLocation.getX(), portalLocation.up().getY(), portalLocation.getZ()));
        }
    }


    private static Optional<BlockPos> findPortalLocation(World worldTo, BlockPos fromPos) {
        if(worldTo.getBlockState(fromPos).getBlock() == JAVDRegistry.PORTAL_BLOCK.get() && isSafeSpawnLocation(worldTo, fromPos)) {
            return Optional.of(fromPos.up());
        }

        int range = 5;
        return Optional.ofNullable(ChunkPos.getAllInBox(worldTo.getChunkAt(fromPos).getPos(), range)
                .map(chunkPos -> worldTo.getChunk(chunkPos.x, chunkPos.z).getTileEntitiesPos())
                .flatMap(Collection::stream)
                .collect(Collectors.toList()).stream()
                .filter(pos -> worldTo.getTileEntity(pos) instanceof PortalTileEntity)
                .findFirst()
                .orElseGet(() -> {
                    BlockPos.Mutable mutableBlockPos = new BlockPos.Mutable(0, 0, 0);
                        for (int y = 0; y < 256; y++) {
                            for (int x = fromPos.getX() - 6; x < fromPos.getX() + 6; x++) {
                                for (int z = fromPos.getZ() - 6; z < fromPos.getZ() + 6; z++) {
                                    mutableBlockPos.setPos(x, y, z);
                                    BlockState blockState = worldTo.getBlockState(mutableBlockPos);
                                    if (blockState.isAir() && isSafeSpawnLocation(worldTo, mutableBlockPos.up())) {
                                        return mutableBlockPos;
                                    }
                                }
                            }
                        }
                    return null;
                }));

    }


    private static boolean isSafeSpawnLocation(World world, BlockPos blockPos) {
        return world.getBlockState(blockPos).isAir() && world.getBlockState(blockPos.up()).isAir();
    }


}
