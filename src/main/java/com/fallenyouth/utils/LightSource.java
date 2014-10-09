package com.fallenyouth.utils;

import net.minecraft.server.v1_7_R3.EnumSkyBlock;
import net.minecraft.server.v1_7_R3.PlayerChunkMap;
import net.minecraft.server.v1_7_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class LightSource {

    private static Method cachedPlayerChunk;
    private static Field cachedDirtyField;

    // For choosing an adjacent air block
    private static BlockFace[] SIDES = {
            BlockFace.UP, BlockFace.DOWN, BlockFace.NORTH,
            BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST };

    /**
     * Create light with level at a location.
     * @param loc - which block to update.
     * @param level - the new light level.
     */
    public static void createLightSource(Location loc, int level) {
        WorldServer nmsWorld = ((CraftWorld) loc.getWorld()).getHandle();
        int oldLevel = loc.getBlock().getLightLevel();

        // Sets the light source at the location to the level
        nmsWorld.b(EnumSkyBlock.BLOCK, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), level);

        // Send packets to the area telling players to see this level
        updateChunk(nmsWorld, loc);

        // If you comment this out it is more likely to get light sources you can't remove
        // but if you do comment it, light is consistent on relog and what not.
        nmsWorld.b(EnumSkyBlock.BLOCK, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), oldLevel);
    }

    private static Block getAdjacentAirBlock(Block block) {
        // Find the first adjacent air block
        for (BlockFace face : SIDES) {
            // Don't use these sides
            if (block.getY() == 0x0 && face == BlockFace.DOWN)
                continue;
            if (block.getY() == 0xFF && face == BlockFace.UP)
                continue;

            Block candidate = block.getRelative(face);

            if (candidate.getType().isTransparent()) {
                return candidate;
            }
        }
        return block;
    }

    /**
     * Gets all the chunks touching/diagonal to the chunk the location is in and updates players with them.
     * @param loc - location to the block that was updated.
     */
    @SuppressWarnings("rawtypes")
    private static void updateChunk(WorldServer nmsWorld, Location loc) {
        try {
            PlayerChunkMap map = nmsWorld.getPlayerChunkMap();

            // Update the light itself
            Block adjacent = getAdjacentAirBlock(loc.getBlock());
            nmsWorld.A(adjacent.getX(), adjacent.getY(), adjacent.getZ());

            int chunkX = loc.getBlockX() >> 4;
            int chunkZ = loc.getBlockZ() >> 4;

            // Make sure the block itself is marked
            map.flagDirty(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

            // See if the current segment can be updated
            Object playerChunk = getPlayerCountMethod().invoke(map, chunkX, chunkZ, false);

            if (playerChunk != null) {
                Field dirtyField = getDirtyField(playerChunk);
                int dirtyCount = (Integer) dirtyField.get(playerChunk);

                // Minecraft will automatically send out a Packet51MapChunk for us,
                // with only those segments (16x16x16) that are needed.
                if (dirtyCount > 0) {
                    dirtyField.set(playerChunk, 64);
                }
            }

            map.flush();

        } catch (SecurityException e) {
            throw new RuntimeException("Access denied", e);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Reflection problem.", e);
        }
    }

    private static Method getPlayerCountMethod() throws NoSuchMethodException, SecurityException {
        if (cachedPlayerChunk == null) {
            cachedPlayerChunk = PlayerChunkMap.class.getDeclaredMethod("a", int.class, int.class, boolean.class);
            cachedPlayerChunk.setAccessible(true);
        }
        return cachedPlayerChunk;
    }

    private static Field getDirtyField(Object playerChunk) throws NoSuchFieldException, SecurityException {
        if (cachedDirtyField == null) {
            cachedDirtyField = playerChunk.getClass().getDeclaredField("dirtyCount");
            cachedDirtyField.setAccessible(true);
        }
        return cachedDirtyField;
    }
}