// ChunkFinder.java
// PURPOSE: single-player / admin-only utility: scan loaded chunks for target deepslate blocks
// NOTE: adapt registration to Fabric/Forge; do NOT use for multiplayer cheating.

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.BufferRenderer;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;
import java.util.List;

public class ChunkFinder {
    private final MinecraftClient mc = MinecraftClient.getInstance();

    // Target block types to detect (example)
    private final List<Block> targetBlocks = Arrays.asList(
            Blocks.COBBLED_DEEPSLATE,
           
            Blocks.CRACKED_DEEPSLATE,
            
            Blocks.DEEPSLATE // add other deepslate variants as needed
    );

    // Chunks that currently contain at least one target block
    private final Set<ChunkPos> matchingChunks = new HashSet<>();

    // Call periodically (e.g., every few ticks) while in-game to update scanning
    public void scanLoadedChunks() {
        if (mc.world == null || mc.player == null) return;

        matchingChunks.clear();

        // get player's view distance or define radius
        int viewRadiusChunks = mc.options.viewDistance; // approximate; adapt if needed
        int playerChunkX = mc.player.getBlockPos().getX() >> 4;
        int playerChunkZ = mc.player.getBlockPos().getZ() >> 4;

        // iterate loaded chunk range around player
        for (int cx = playerChunkX - viewRadiusChunks; cx <= playerChunkX + viewRadiusChunks; cx++) {
            for (int cz = playerChunkZ - viewRadiusChunks; cz <= playerChunkZ + viewRadiusChunks; cz++) {
                ChunkPos cpos = new ChunkPos(cx, cz);
                // try to get the chunk (client chunk provider)
                if (chunkContainsTarget(mc.world, cpos)) {
                    matchingChunks.add(cpos);
                }
            }
        }
    }

    // Scans blocks inside the chunk for target blocks.
    // For performance: sample or restrict Y-range depending on what you're looking for.
    private boolean chunkContainsTarget(World world, ChunkPos cpos) {
        // Get chunk start positions
        int startX = (cpos.x << 4);
        int startZ = (cpos.z << 4);

        // Choose Y-range you want to search. Deepslate is typically below Y=0..16 depending on version.
        int minY = 0;   // adjust to version
        int maxY = 64;  // keep limited for speed; adjust as needed

        // For performance you can sample instead of scanning every block.
        for (int x = 0; x < 16; x++) {
            for (int z = 0; z < 16; z++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(startX + x, y, startZ + z);
                    Block b = world.getBlockState(pos).getBlock();
                    if (isTargetBlock(b)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isTargetBlock(Block b) {
        return targetBlocks.contains(b);
    }

    // Call from a world render event to draw overlays
    public void renderChunkOverlays(MatrixStack matrices, float tickDelta) {
        if (mc.world == null || mc.player == null) return;

        double camX = mc.gameRenderer.getCamera().getPos().x;
        double camY = mc.gameRenderer.getCamera().getPos().y;
        double camZ = mc.gameRenderer.getCamera().getPos().z;

        // Configure color & alpha
        float r = 0.0f, g = 1.0f, b = 0.0f, a = 0.25f; // semi-transparent green fill
        float outlineR = 0.0f, outlineG = 1.0f, outlineB = 0.0f, outlineA = 0.9f;

        // For each matching chunk draw a box that covers chunk boundaries
        for (ChunkPos cp : matchingChunks) {
            double minX = (cp.x << 4) - camX;
            double minZ = (cp.z << 4) - camZ;
            double maxX = ((cp.x << 4) + 16) - camX;
            double maxZ = ((cp.z << 4) + 16) - camZ;
            double minY = mc.world.getBottomY() - camY; // world min Y
            double maxY = mc.world.getTopY() - camY;    // world max Y

            // Render fill (flat rectangle at player Y or full-box depending on preference)
            // Here we render an outline box around the entire chunk column for visibility.

            // Note: Rendering helper code differs between versions. Below is conceptual.
            WorldRenderer.drawBox(matrices, /* vertex consumer */ mc.getBufferBuilders().getEntityVertexConsumers(), 
                                  minX, minY, minZ, maxX, maxY, maxZ,
                                  r, g, b, a, // fill color (some APIs use separate fill)
                                  outlineR, outlineG, outlineB, outlineA); // outline color
        }
    }
}
