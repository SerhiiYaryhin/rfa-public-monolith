package media.toloka.rfa.service;

public class RFABitUtils {

    // ==== INT ====
    public static int setBit(int value, int bitIndex) {
        return value | (1 << bitIndex);
    }

    public static int clearBit(int value, int bitIndex) {
        return value & ~(1 << bitIndex);
    }

    public static int toggleBit(int value, int bitIndex) {
        return value ^ (1 << bitIndex);
    }

    public static boolean isBitSet(int value, int bitIndex) {
        return ((value >> bitIndex) & 1) == 1;
    }

    // ==== LONG ====
    public static long setBit(long value, int bitIndex) {
        return value | (1L << bitIndex);
    }

    public static long clearBit(long value, int bitIndex) {
        return value & ~(1L << bitIndex);
    }

    public static long toggleBit(long value, int bitIndex) {
        return value ^ (1L << bitIndex);
    }

    public static boolean isBitSet(long value, int bitIndex) {
        return ((value >> bitIndex) & 1L) == 1L;
    }

    // ==== BYTE ====
    public static byte setBit(byte value, int bitIndex) {
        return (byte) (value | (1 << bitIndex));
    }

    public static byte clearBit(byte value, int bitIndex) {
        return (byte) (value & ~(1 << bitIndex));
    }

    public static byte toggleBit(byte value, int bitIndex) {
        return (byte) (value ^ (1 << bitIndex));
    }

    public static boolean isBitSet(byte value, int bitIndex) {
        return ((value >> bitIndex) & 1) == 1;
    }
}
