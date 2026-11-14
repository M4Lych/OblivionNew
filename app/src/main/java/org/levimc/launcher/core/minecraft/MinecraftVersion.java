package org.levimc.launcher.core.minecraft;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MinecraftVersion {
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:\\.\\d+)*");

    private MinecraftVersion() {}

    public static boolean isAtLeast(String current, String minimum) {
        int[] left = components(current);
        int[] right = components(minimum);
        if (left.length == 0 || right.length == 0) return false;
        for (int i = 0; i < Math.max(left.length, right.length); i++) {
            int a = i < left.length ? left[i] : 0;
            int b = i < right.length ? right[i] : 0;
            if (a != b) return a > b;
        }
        return true;
    }

    private static int[] components(String value) {
        if (value == null) return new int[0];
        Matcher match = NUMBER.matcher(value);
        if (!match.find()) return new int[0];
        String[] parts = match.group().split("\\.");
        try {
            int major = Integer.parseInt(parts[0]);
            // Bedrock release labels may appear either as 1.26.x.x or 26.x.x.
            // Two-digit major values represent the minor component under major version 1.
            int prefix = major >= 10 && major < 100 ? 1 : 0;
            int[] result = new int[parts.length + prefix];
            if (prefix != 0) result[0] = 1;
            for (int i = 0; i < parts.length; i++) {
                result[i + prefix] = Integer.parseInt(parts[i]);
            }
            return result;
        } catch (NumberFormatException error) {
            return new int[0];
        }
    }
}
