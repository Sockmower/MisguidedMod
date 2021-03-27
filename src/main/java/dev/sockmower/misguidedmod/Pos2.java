package dev.sockmower.misguidedmod;

public class Pos2 {
    public final int x, z;
    public boolean poisoned;

    public static Pos2 fromLong(long l) {
        return new Pos2((int) l, (int) (l >> 32));
    }

    public Pos2(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public Pos2(boolean poisoned) {
        this.x = -1;
        this.z = -1;
        this.poisoned = poisoned;
    }

    public long asLong() {
        return (x & 0xffffffffL) << 32 | z & 0xffffffffL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Pos2 chunkPos = (Pos2) o;
        return x == chunkPos.x && z == chunkPos.z;
    }

    // 31 is actually a pretty terrible prime to use for hashing, minecraft's implementation is a lot better
    @Override
    public int hashCode() {
        return 31 * x + z;
        // mc does it like this:
        //        int lvt_1_1_ = 0x19660d * x + 0x3c6ef35f;
        //        int lvt_2_1_ = 0x19660d * (z ^ 0xdeadbeef) + 0x3c6ef35f;
        //        return lvt_1_1_ ^ lvt_2_1_;
    }

    @Override
    public String toString() {
        return String.format("Pos2{%d, %d}", x, z);
    }

    public int chebyshevDistance(Pos2 to) {
        return Math.max(Math.abs(to.x - x), Math.abs(to.z - z));
    }

    public int euclidDistanceSq(Pos2 to) {
        int dx = to.x - x;
        int dz = to.z - z;
        return dx * dx + dz * dz;
    }

    public int taxicabDistance(Pos2 to) {
        return Math.abs(to.x - x) + Math.abs(to.z - z);
    }

    public static Pos2 chunkPosFromBlockPos(double x, double z) {
        return new Pos2(
                ((int) x) >> 4,
                ((int) z) >> 4);
    }
}
