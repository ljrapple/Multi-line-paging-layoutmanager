package com.ljr.com.multi;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DataGenerator {
    private final static int a_VALUE = 97;
    private final static int A_VALUE = 65;
    private final static int z_VALUE = 122;
    private final static int Z_VALUE = 90;
    private final static int MAX_VALUE = 123;

    public static List<String> getStringsData(int size) {
        List<String> list = new ArrayList<>();
        Random random = new Random();
        for (int i = 0; i < size; i++) {
            int stringSize = Math.max(10, random.nextInt(20));
            list.add(generateString(i, stringSize));
        }
        return list;
    }

    private static String generateString(int pos, int size) {
        StringBuilder sb = new StringBuilder(pos +" : ");
        Random random = new Random();
        int value;
        for (int i = 0; i < size; i++) {
            value = random.nextInt(MAX_VALUE);
            if ((value >= A_VALUE && value <= Z_VALUE) || (value >= a_VALUE && value <= z_VALUE)) {
                sb.append(toChar(value));
            } else {
                i--;
            }
        }
        return sb.toString();
    }

    private static char toChar(int data) {
        return (char) data;
    }
}
