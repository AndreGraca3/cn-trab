package pt.isel.utils;

import java.util.ArrayList;

public class ArrayUtils {
    public static byte[] toByteArray(ArrayList<Byte> data) {
        byte[] result = new byte[data.size()];
        for (int i = 0; i < data.size(); i++) {
            result[i] = data.get(i);
        }
        return result;
    }
}
