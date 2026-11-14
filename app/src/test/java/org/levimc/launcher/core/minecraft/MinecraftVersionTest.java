package org.levimc.launcher.core.minecraft;

import org.junit.Test;
import static org.junit.Assert.*;

public class MinecraftVersionTest {
    @Test public void acceptsBedrock26PrefixedVersion() {
        assertTrue(MinecraftVersion.isAtLeast("26.10.0", "1.26.0.0"));
    }
    @Test public void acceptsConventionalVersion() {
        assertTrue(MinecraftVersion.isAtLeast("1.26.0.0", "1.26.0.0"));
        assertTrue(MinecraftVersion.isAtLeast("1.26.1.0", "1.26.0.0"));
    }
    @Test public void rejectsOlderVersion() {
        assertFalse(MinecraftVersion.isAtLeast("1.21.80.3", "1.26.0.0"));
        assertFalse(MinecraftVersion.isAtLeast("25.90.0", "1.26.0.0"));
    }
    @Test public void rejectsUnknownVersion() {
        assertFalse(MinecraftVersion.isAtLeast("unknown", "1.26.0.0"));
    }
}
