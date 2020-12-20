package kr.co.gstech.smarttimer.utils;

public final class StLibrary {

    private StLibrary() {
        throw new IllegalStateException("Utility class");
    }

    public static void decimalToHex(byte[] dArr, int bytes) {
        for (int i = 0; i < bytes; i++) {
            dArr[i] = (byte) (dArr[i] & 0xff);
        }
    }


}
