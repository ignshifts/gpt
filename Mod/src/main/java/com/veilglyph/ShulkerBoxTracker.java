package com.veilglyph;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.mob.PiglinEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.world.chunk.WorldChunk;

import java.util.*;

public class ShulkerBoxTracker {

    public static volatile List<TrackedEntry> tracked = Collections.emptyList();

    private static int tickCounter = 0;
    private static int blockScanPhase = 0;
    private static List<TrackedEntry> lastBlockScanEntries = Collections.emptyList();
    private static final int SCAN_INTERVAL = 40; // block entities + entities every 2s
    private static final int BLOCK_SCAN_EVERY = 2; // full-chunk block scan every 2nd cycle (80 ticks) to reduce lag

    public enum TrackedType {
        SHULKER_BOX, ENDER_CHEST, PISTON, STICKY_PISTON, HOPPER, DISPENSER, BARREL, CHEST, SIGN, ITEM_FRAME, SPAWNER, VILLAGER, ANCIENT_DEBRIS,
        GILDED_BLACKSTONE, ENCHANTING_TABLE, NETHERITE_BLOCK, DIAMOND_BLOCK, CONDUIT, PIGLIN,
        END_PORTAL_FRAME, END_PORTAL,
        DEEPSLATE_EMERALD_ORE, EMERALD_ORE,
        IRON_ORE, DEEPSLATE_IRON_ORE, DIAMOND_ORE, DEEPSLATE_DIAMOND_ORE,
        GOLD_ORE, DEEPSLATE_GOLD_ORE, REDSTONE_ORE, DEEPSLATE_REDSTONE_ORE
    }

    public record TrackedEntry(BlockPos pos, double distance, TrackedType type, DyeColor shulkerColor) {}

    public static void tick(MinecraftClient client) {
        if (!VeilglyphMod.modEnabled) {
            tracked = Collections.emptyList();
            return;
        }
        if (client.world == null || client.player == null) {
            tracked = Collections.emptyList();
            return;
        }

        tickCounter++;
        if (tickCounter < SCAN_INTERVAL) return;
        tickCounter = 0;
        blockScanPhase = (blockScanPhase + 1) % BLOCK_SCAN_EVERY;

        scan(client, blockScanPhase == 0);
    }

    private static void scan(MinecraftClient client, boolean runBlockScan) {
        ClientWorld world = client.world;
        if (world == null || client.player == null) return;

        BlockPos centerPos;
        if (com.veilglyph.nimbus.NimbusConfig.trackerUseFreecamPosition
                && com.veilglyph.nimbus.FreecamHandler.active) {
            Vec3d cam = com.veilglyph.nimbus.FreecamHandler.getCamPos();
            centerPos = BlockPos.ofFloored(cam.x, cam.y, cam.z);
        } else {
            centerPos = client.player.getBlockPos();
        }

        List<TrackedEntry> found = new ArrayList<>();
        List<TrackedEntry> blockScanFound = runBlockScan ? new ArrayList<>() : null;
        int renderDistance = client.options.getViewDistance().getValue();
        ChunkPos playerChunk = new ChunkPos(centerPos);

        // Block entities (fast path — always run)
        for (int cx = playerChunk.x - renderDistance; cx <= playerChunk.x + renderDistance; cx++) {
            for (int cz = playerChunk.z - renderDistance; cz <= playerChunk.z + renderDistance; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) continue;

                for (Map.Entry<BlockPos, BlockEntity> be : chunk.getBlockEntities().entrySet()) {
                    BlockPos pos = be.getKey();
                    double distance = Math.sqrt(centerPos.getSquaredDistance(pos));
                    BlockEntity entity = be.getValue();

                    if (entity instanceof ShulkerBoxBlockEntity && TrackerOptions.isEnabled(TrackedType.SHULKER_BOX)) {
                        DyeColor color = null;
                        BlockState state = chunk.getBlockState(pos);
                        if (state.getBlock() instanceof ShulkerBoxBlock sb) color = sb.getColor();
                        found.add(new TrackedEntry(pos, distance, TrackedType.SHULKER_BOX, color));
                    } else if (entity instanceof EnderChestBlockEntity && TrackerOptions.isEnabled(TrackedType.ENDER_CHEST)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.ENDER_CHEST, null));
                    } else if (entity instanceof HopperBlockEntity && TrackerOptions.isEnabled(TrackedType.HOPPER)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.HOPPER, null));
                    } else if (entity instanceof DispenserBlockEntity && TrackerOptions.isEnabled(TrackedType.DISPENSER)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.DISPENSER, null));
                    } else if (entity instanceof BarrelBlockEntity && TrackerOptions.isEnabled(TrackedType.BARREL)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.BARREL, null));
                    } else if (entity instanceof ChestBlockEntity && TrackerOptions.isEnabled(TrackedType.CHEST)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.CHEST, null));
                    } else if (entity instanceof SignBlockEntity && TrackerOptions.isEnabled(TrackedType.SIGN)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.SIGN, null));
                    } else if (entity instanceof MobSpawnerBlockEntity && TrackerOptions.isEnabled(TrackedType.SPAWNER)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.SPAWNER, null));
                    } else if (entity instanceof PistonBlockEntity && (TrackerOptions.isEnabled(TrackedType.PISTON) || TrackerOptions.isEnabled(TrackedType.STICKY_PISTON))) {
                        // PistonBlockEntity = extended piston head (same fast iteration as hoppers)
                        found.add(new TrackedEntry(pos, distance, TrackedType.PISTON, null));
                    } else if (entity instanceof EnchantingTableBlockEntity && TrackerOptions.isEnabled(TrackedType.ENCHANTING_TABLE)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.ENCHANTING_TABLE, null));
                    } else if (entity instanceof ConduitBlockEntity && TrackerOptions.isEnabled(TrackedType.CONDUIT)) {
                        found.add(new TrackedEntry(pos, distance, TrackedType.CONDUIT, null));
                    }
                }

                // Block-only types (no block entity): run every BLOCK_SCAN_EVERY cycles to reduce lag
                if (!runBlockScan) continue;

                // Ancient debris (block only — no block entity; Nether-only, Y 8–119 to reduce lag)
                if (TrackerOptions.isEnabled(TrackedType.ANCIENT_DEBRIS) && world.getRegistryKey() == World.NETHER) {
                    ChunkPos cPos = chunk.getPos();
                    int baseX = cPos.getStartX();
                    int baseZ = cPos.getStartZ();
                    int minY = Math.max(8, world.getBottomY());
                    int maxY = Math.min(119, world.getTopYInclusive());
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                                if (chunk.getBlockState(pos).getBlock() == Blocks.ANCIENT_DEBRIS) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.ANCIENT_DEBRIS, null));
                                }
                            }
                        }
                    }
                }

                // End portal frame + end portal (block only; Overworld-only; limit Y-range to reduce lag)
                boolean needFrame = TrackerOptions.isEnabled(TrackedType.END_PORTAL_FRAME);
                boolean needPortal = TrackerOptions.isEnabled(TrackedType.END_PORTAL);
                if ((needFrame || needPortal) && world.getRegistryKey() == World.OVERWORLD) {
                    ChunkPos cPos = chunk.getPos();
                    int baseX = cPos.getStartX();
                    int baseZ = cPos.getStartZ();
                    int minY = Math.max(world.getBottomY(), -32);
                    int maxY = Math.min(world.getTopYInclusive(), 96);
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                                var block = chunk.getBlockState(pos).getBlock();
                                if (needFrame && block == Blocks.END_PORTAL_FRAME) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.END_PORTAL_FRAME, null));
                                } else if (needPortal && block == Blocks.END_PORTAL) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.END_PORTAL, null));
                                }
                            }
                        }
                    }
                }

                // Gilded blackstone, netherite block, diamond block, ores (no block entities — full chunk scan when enabled)
                boolean needGilded = TrackerOptions.isEnabled(TrackedType.GILDED_BLACKSTONE);
                boolean needNetherite = TrackerOptions.isEnabled(TrackedType.NETHERITE_BLOCK);
                boolean needDiamond = TrackerOptions.isEnabled(TrackedType.DIAMOND_BLOCK);
                boolean needDeepslateEmerald = TrackerOptions.isEnabled(TrackedType.DEEPSLATE_EMERALD_ORE);
                boolean needEmerald = TrackerOptions.isEnabled(TrackedType.EMERALD_ORE);
                boolean needIron = TrackerOptions.isEnabled(TrackedType.IRON_ORE);
                boolean needDeepslateIron = TrackerOptions.isEnabled(TrackedType.DEEPSLATE_IRON_ORE);
                boolean needDiamondOre = TrackerOptions.isEnabled(TrackedType.DIAMOND_ORE);
                boolean needDeepslateDiamond = TrackerOptions.isEnabled(TrackedType.DEEPSLATE_DIAMOND_ORE);
                boolean needGold = TrackerOptions.isEnabled(TrackedType.GOLD_ORE);
                boolean needDeepslateGold = TrackerOptions.isEnabled(TrackedType.DEEPSLATE_GOLD_ORE);
                boolean needRedstone = TrackerOptions.isEnabled(TrackedType.REDSTONE_ORE);
                boolean needDeepslateRedstone = TrackerOptions.isEnabled(TrackedType.DEEPSLATE_REDSTONE_ORE);
                if (needGilded || needNetherite || needDiamond || needDeepslateEmerald || needEmerald
                        || needIron || needDeepslateIron || needDiamondOre || needDeepslateDiamond
                        || needGold || needDeepslateGold || needRedstone || needDeepslateRedstone) {
                    ChunkPos cPos = chunk.getPos();
                    int baseX = cPos.getStartX();
                    int baseZ = cPos.getStartZ();
                    int minY = world.getBottomY();
                    int maxY = world.getTopYInclusive();
                    for (int x = 0; x < 16; x++) {
                        for (int z = 0; z < 16; z++) {
                            for (int y = minY; y <= maxY; y++) {
                                BlockPos pos = new BlockPos(baseX + x, y, baseZ + z);
                                var block = chunk.getBlockState(pos).getBlock();
                                if (needGilded && block == Blocks.GILDED_BLACKSTONE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.GILDED_BLACKSTONE, null));
                                } else if (needNetherite && block == Blocks.NETHERITE_BLOCK) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.NETHERITE_BLOCK, null));
                                } else if (needDiamond && block == Blocks.DIAMOND_BLOCK) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DIAMOND_BLOCK, null));
                                } else if (needDeepslateEmerald && block == Blocks.DEEPSLATE_EMERALD_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DEEPSLATE_EMERALD_ORE, null));
                                } else if (needEmerald && block == Blocks.EMERALD_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.EMERALD_ORE, null));
                                } else if (needIron && block == Blocks.IRON_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.IRON_ORE, null));
                                } else if (needDeepslateIron && block == Blocks.DEEPSLATE_IRON_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DEEPSLATE_IRON_ORE, null));
                                } else if (needDiamondOre && block == Blocks.DIAMOND_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DIAMOND_ORE, null));
                                } else if (needDeepslateDiamond && block == Blocks.DEEPSLATE_DIAMOND_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DEEPSLATE_DIAMOND_ORE, null));
                                } else if (needGold && block == Blocks.GOLD_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.GOLD_ORE, null));
                                } else if (needDeepslateGold && block == Blocks.DEEPSLATE_GOLD_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DEEPSLATE_GOLD_ORE, null));
                                } else if (needRedstone && block == Blocks.REDSTONE_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.REDSTONE_ORE, null));
                                } else if (needDeepslateRedstone && block == Blocks.DEEPSLATE_REDSTONE_ORE) {
                                    blockScanFound.add(new TrackedEntry(pos, Math.sqrt(centerPos.getSquaredDistance(pos)), TrackedType.DEEPSLATE_REDSTONE_ORE, null));
                                }
                            }
                        }
                    }
                }
            }
        }

        if (runBlockScan && blockScanFound != null) {
            lastBlockScanEntries = new ArrayList<>(blockScanFound);
            found.addAll(blockScanFound);
        } else {
            found.addAll(lastBlockScanEntries);
        }

        // Item frames & villagers (entities)
        Box box = new Box(centerPos).expand(renderDistance * 16);
        if (TrackerOptions.isEnabled(TrackedType.ITEM_FRAME)) {
            for (Entity e : world.getEntitiesByClass(ItemFrameEntity.class, box, x -> true)) {
                BlockPos pos = e.getBlockPos();
                double distance = Math.sqrt(centerPos.getSquaredDistance(pos));
                found.add(new TrackedEntry(pos, distance, TrackedType.ITEM_FRAME, null));
            }
        }
        if (TrackerOptions.isEnabled(TrackedType.VILLAGER)) {
            for (Entity e : world.getEntitiesByClass(VillagerEntity.class, box, x -> true)) {
                BlockPos pos = e.getBlockPos();
                double distance = Math.sqrt(centerPos.getSquaredDistance(pos));
                found.add(new TrackedEntry(pos, distance, TrackedType.VILLAGER, null));
            }
        }
        if (TrackerOptions.isEnabled(TrackedType.PIGLIN)) {
            for (Entity e : world.getEntitiesByClass(PiglinEntity.class, box, x -> true)) {
                BlockPos pos = e.getBlockPos();
                double distance = Math.sqrt(centerPos.getSquaredDistance(pos));
                found.add(new TrackedEntry(pos, distance, TrackedType.PIGLIN, null));
            }
        }

        found.sort(Comparator.comparingDouble(TrackedEntry::distance));
        tracked = Collections.unmodifiableList(found);
    }
}
