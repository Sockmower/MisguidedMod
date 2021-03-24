package dev.sockmower.misguidedmod;

import java.net.SocketAddress;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import net.minecraft.network.Packet;
import net.minecraft.network.play.server.SPacketChunkData;
import net.minecraft.network.play.server.SPacketUnloadChunk;

/**
 * Hook into Minecraft's packet pipeline
 * to filter out unload packets to keep chunks rendered
 */
@ChannelHandler.Sharable
public class PacketHandler extends SimpleChannelInboundHandler<Packet<?>> implements ChannelOutboundHandler {

    static final String NAME = MisguidedMod.MODID + ":packet_handler";

    private final MisguidedMod moreChunks;

    public PacketHandler(MisguidedMod moreChunks) {
        this.moreChunks = moreChunks;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Packet<?> packet) throws Exception {
        if (packet instanceof SPacketUnloadChunk) {
            return; // ignore packet, we manually unload our chunks
        }
        if (packet instanceof SPacketChunkData) {
            try {
                final SPacketChunkData chunkPacket = (SPacketChunkData) packet;
                if (chunkPacket.isFullChunk()) {
                    // full chunk, not just a section
                    final Pos2 pos = new Pos2(chunkPacket.getChunkX(), chunkPacket.getChunkZ());
                    moreChunks.onReceiveGameChunk(new CachedChunk(pos, chunkPacket));
                    return; // drop packet, we load it manually
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        ctx.fireChannelRead(packet);
    }

    @Override
    public void bind(ChannelHandlerContext ctx, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.bind(localAddress, promise);
    }

    @Override
    public void connect(ChannelHandlerContext ctx, SocketAddress remoteAddress, SocketAddress localAddress, ChannelPromise promise) throws Exception {
        ctx.connect(remoteAddress, localAddress, promise);
    }

    @Override
    public void disconnect(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.disconnect(promise);
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.close(promise);
    }

    @Override
    public void deregister(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        ctx.deregister(promise);
    }

    @Override
    public void read(ChannelHandlerContext ctx) throws Exception {
        ctx.read();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        ctx.write(msg, promise);
    }

    @Override
    public void flush(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
