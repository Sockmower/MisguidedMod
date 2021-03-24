package dev.sockmower.misguidedmod;

import net.minecraft.network.play.server.SPacketChunkData;

public class CachedChunk {
    public final Pos2 pos;
    public final SPacketChunkData packet;

    @Override
    public String toString() {
        return String.format("Chunk{%s}", pos);
    }

    public CachedChunk(Pos2 pos, SPacketChunkData packet) {
        this.pos = pos;
        this.packet = packet;
    }
}
