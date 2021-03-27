package dev.sockmower.misguidedmod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.client.Minecraft;
import net.minecraft.network.play.server.SPacketChunkData;
import org.apache.logging.log4j.Logger;
import sun.misc.Cache;

public class CachedWorld {
    private final String directory;
    private Logger logger;
    private Minecraft mc;
    private MisguidedMod mm;

    private Map<String, CachedRegion> regionCache = new ConcurrentHashMap<>();
    private final Set<Pos2> desiredChunks = ConcurrentHashMap.newKeySet();
    private final Set<CachedChunk> chunkWriteQueue = ConcurrentHashMap.newKeySet();

    CachedWorld(Path directory, Logger logger, Minecraft mc, MisguidedMod mm) {
        this.logger = logger;
        if (!Files.exists(directory)) {
            try {
                Files.createDirectories(directory);
            } catch (IOException ignored) {
                logger.error(ignored);
            }
        }
        this.mc = mc;
        this.mm = mm;
        this.directory = directory.toString();
        logger.info("Initialized cachedWorld with working dir as {}", directory);

        Thread chunkLoaderThread = new Thread(() -> {
            while (true) {
                if (!desiredChunks.isEmpty()) {
                    int waitTime = 1000 / desiredChunks.size();
                    for (Pos2 pos : desiredChunks) {
                        if (pos.poisoned) { logger.info("Exiting chunk load thread"); return; }
                        try {
                            CachedRegion reg = getCachedRegion(pos.x >> 4, pos.z >> 4);
                            CachedChunk x = reg.getChunk(pos);
                            if (x != null) {
                                mc.addScheduledTask(() -> mm.loadChunk(x));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            Thread.sleep(waitTime);
                        } catch (Exception ignored) {}
                    }
                    desiredChunks.clear();
                } else {
                    try {
                        Thread.sleep(250);
                    } catch (Exception ignored) {}
                }
            }
        }, "MisguidedMod Chunk Loading Thread");
        chunkLoaderThread.start();

        Thread chunkWriterThread = new Thread(() -> {
            while (true) {
                if (!chunkWriteQueue.isEmpty()) {
                    int waitTime = 1000 / chunkWriteQueue.size();
                    for (CachedChunk chunk : chunkWriteQueue) {
                        if (chunk.poison) { logger.info("Exiting chunk write thread"); return; }
                        try {
                            CachedRegion reg = getCachedRegion(chunk.pos.x >> 4, chunk.pos.z >> 4);
                            reg.writeChunk(chunk);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        try {
                            Thread.sleep(waitTime);
                        } catch (Exception ignored) {}
                    }
                    chunkWriteQueue.clear();
                } else {
                    try {
                        Thread.sleep(250);
                    } catch (Exception ignored) {}
                }
            }
        }, "MisguidedMod Chunk Writing Thread");
        chunkWriterThread.start();

        Thread regionPruningThread = new Thread(() -> {
            while (true) {
                try {
                    for (String key : regionCache.keySet()) {
                        CachedRegion reg = regionCache.get(key);
                        if (reg.poison) { logger.info("Exiting region prune thread"); return; }
                        if (System.currentTimeMillis() - reg.lastAccessed > 60000) {
                            reg.close();
                            regionCache.remove(key);
                        }
                    }
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "MisguidedMod Region Pruning Thread");
        regionPruningThread.start();
    }

    public CachedRegion getCachedRegion(int x, int z) throws IOException {
        String key = String.format("%d.%d", x, z);
        CachedRegion reg;
        if (!regionCache.containsKey(key)) {
            reg = new CachedRegion(directory, x, z, logger);
            regionCache.put(key, reg);
        } else {
            reg = regionCache.get(key);
        }
        reg.lastAccessed = System.currentTimeMillis();
        return reg;
    }

    public void addChunksToLoadQueue(Set<Pos2> positions) throws IOException {
        desiredChunks.addAll(positions);
    }

    public void writeChunk(CachedChunk chunk) throws IOException {
        chunkWriteQueue.add(chunk);
    }

    public void releaseFiles() throws IOException {
        for (CachedRegion reg : regionCache.values()) {
            reg.close();
        }
        regionCache.clear();
    }

    public void cancelThreads() {
        CachedChunk poisonedChunk = new CachedChunk(new Pos2(-1, -1), new SPacketChunkData());
        poisonedChunk.poison();
        chunkWriteQueue.add(poisonedChunk);

        Pos2 poisonedPos = new Pos2(true);
        desiredChunks.add(poisonedPos);

        CachedRegion poisonedReg = new CachedRegion(true);
        regionCache.put("_poison", poisonedReg);
    }
}
