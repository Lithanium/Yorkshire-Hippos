package reechee;

public class Bit {
    public static boolean get(int intRepresentation, int position) {
        return ((intRepresentation) & (1 << (position))) != 0;
    }

    public static short write(short intRepresentation, int position, boolean value) {
        boolean[] flags = new boolean[16];
        for (int i = 0; i < 16; i++) {
            flags[i] = (intRepresentation & (1 << i)) != 0;
        }

        flags[position] = value;

        intRepresentation = 0;
        for (int i = 0; i < 16; i++) {
            if (flags[i]) intRepresentation |= (1 << i);
        }
        return intRepresentation;
    }
}
