package dev.sockmower.misguidedmod;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.client.multiplayer.ServerData;
import org.apache.logging.log4j.Logger;

import io.netty.channel.ChannelPipeline;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.play.server.SPacketUnloadChunk;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.FMLClientHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

@Mod(modid = MisguidedMod.MODID, name = MisguidedMod.NAME, version = MisguidedMod.VERSION, clientSideOnly = true)
public class MisguidedMod {
    public static final String MODID = "misguidedmod";
    public static final String NAME = "Just A Misguided Mod";
    public static final String VERSION = "1.0";

    private static final Minecraft mc = Minecraft.getMinecraft();
    public static Logger logger;
    private final Set<Pos2> loadedChunks = new HashSet<Pos2>();
    private static CachedWorld cachedWorld;
    private long lastExtraTime = 0;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
    }

    @EventHandler
    public void init(FMLInitializationEvent event) {
        // some example code
        logger.info("Initializing MisguidedMod v{}", VERSION);
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void insertPacketHandler() {
        NetHandlerPlayClient mcConnection = (NetHandlerPlayClient) FMLClientHandler.instance().getClientPlayHandler();
        if (mcConnection == null) {
            logger.error("Could not inject packet handler into pipeline: mc.connection == null");
            return;
        }

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();

        if (pipe.get(PacketHandler.NAME) != null) {
            logger.warn("game server connection pipeline already contains handler, removing and re-adding");
            pipe.remove(PacketHandler.NAME);
        }

        PacketHandler packetHandler = new PacketHandler(this);
        pipe.addBefore("fml:packet_handler", PacketHandler.NAME, packetHandler);

        logger.info("Packet handler inserted");
    }

    public void removePacketHandler() {
        NetHandlerPlayClient mcConnection = (NetHandlerPlayClient) FMLClientHandler.instance().getClientPlayHandler();
        if (mcConnection == null) {
            logger.error("Could not inject packet handler into pipeline: mc.connection == null");
            return;
        }

        ChannelPipeline pipe = mcConnection.getNetworkManager().channel().pipeline();

        if (pipe.get(PacketHandler.NAME) != null) {
            pipe.remove(PacketHandler.NAME);
        }
    }

    public void loadChunk(CachedChunk chunk) {
        if (!mc.isCallingFromMinecraftThread()) {
            logger.warn("Calling loadChunk from non-mc thread");
            return;
        }

        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            logger.warn("Connection is null!");
            return;
        }

        unloadChunk(chunk.pos);

        conn.handleChunkData(chunk.packet);
        loadedChunks.add(chunk.pos);
    }

    public void unloadChunk(Pos2 pos) {
        if (!mc.isCallingFromMinecraftThread()) {
            logger.warn("Calling loadChunk from non-mc thread");
            return;
        }

        NetHandlerPlayClient conn = mc.getConnection();
        if (conn == null) {
            logger.warn("Connection is null!");
            return;
        }

        conn.processChunkUnload(new SPacketUnloadChunk(pos.x, pos.z));
        loadedChunks.remove(pos);
    }

    public Pos2 getPlayerChunkPos() {
        if (mc.player == null) {
            return null;
        }
        if (mc.player.posX == 0 && mc.player.posY == 0 && mc.player.posZ == 0) return null;
        if (mc.player.posX == 8.5 && mc.player.posY == 65 && mc.player.posZ == 8.5) return null; // position not set from server yet
        return Pos2.chunkPosFromBlockPos(mc.player.posX, mc.player.posZ);
    }

    public Set<Pos2> getNeededChunkPositions() {
        final int rdClient = mc.gameSettings.renderDistanceChunks + 1;
        final Pos2 player = getPlayerChunkPos();

        final Set<Pos2> loadable = new HashSet<>();

        if (player == null) {
            return loadable;
        }

        for (int x = player.x - rdClient; x <= player.x + rdClient; x++) {
            for (int z = player.z - rdClient; z <= player.z + rdClient; z++) {
                Pos2 chunk = new Pos2(x, z);

                if (1 + player.chebyshevDistance(chunk) <= 7) {
                    // do not load extra chunks inside the server's (admittedly, guessed) render distance,
                    // we expect the server to send game chunks here eventually
                    continue;
                }

                if (!loadedChunks.contains(chunk)) {
                    loadable.add(chunk);
                }
            }
        }

        //logger.info("Want to request {} additional chunks", loadable.size());

        return loadable;
    }

    public void unloadOutOfRangeChunks() {
        final int rdClient = mc.gameSettings.renderDistanceChunks + 1;
        final Pos2 player = getPlayerChunkPos();

        if (player == null) {
            return;
        }

        Set<Pos2> toUnload = new HashSet<>();

        for (Pos2 pos : loadedChunks) {
            if (pos.chebyshevDistance(player) > rdClient) {
                // logger.info("Unloading chunk at {} since it is outside of render distance", pos.toString());
                toUnload.add(pos);
            }
        }

        toUnload.forEach(pos -> mc.addScheduledTask(() -> unloadChunk(pos)));
    }

    public void onReceiveGameChunk(CachedChunk chunk) throws IOException {
        mc.addScheduledTask(() -> loadChunk(chunk));

        if ((System.currentTimeMillis() / 1000) - lastExtraTime > 1) {
            lastExtraTime = System.currentTimeMillis() / 1000;
            cachedWorld.addChunksToLoadQueue(getNeededChunkPositions());
            unloadOutOfRangeChunks();
        }

        cachedWorld.writeChunk(chunk);
    }

    @SubscribeEvent
    public void onGameConnected(FMLNetworkEvent.ClientConnectedToServerEvent event) {
        ServerData currentServerData = mc.getCurrentServerData();
        if (currentServerData == null || !mc.getCurrentServerData().serverIP.equals("play.wynncraft.com")) {
            return;
        }
        logger.info("Connected to server {}, client render distance is {}",
                mc.getCurrentServerData().serverIP,
                mc.gameSettings.renderDistanceChunks);

        insertPacketHandler();

        cachedWorld = new CachedWorld(
                Paths.get(mc.mcDataDir.getAbsolutePath() + "\\misguidedmod\\" + mc.getCurrentServerData().serverIP),
                logger,
                mc,
                this
                );
    }

    @SubscribeEvent
    public void onGameDisconnected(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
        loadedChunks.clear();
        logger.info("loadedChunks cleared.");
        try {
            cachedWorld.releaseFiles();
            cachedWorld.cancelThreads();
        } catch (Exception e) { e.printStackTrace(); }

        removePacketHandler();
    }
}
