package dev.sockmower.misguidedmod;

public class ChunkHeader {
    public int size;
    public int offset;
    public int headerOffset;

    ChunkHeader(int offset, int size, int headerOffset) {
        this.offset = offset;
        this.size = size;
        this.headerOffset = headerOffset;
    }

    boolean chunkExists() {
        return size != 0;
    }
}
