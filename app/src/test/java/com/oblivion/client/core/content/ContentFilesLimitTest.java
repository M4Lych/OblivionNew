package com.oblivion.client.core.content;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ContentFilesLimitTest {
    @Test
    public void copyLimitedCopiesContentAtOrBelowLimit() throws Exception {
        byte[] source = "bedrock-pack".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        long copied = ContentFiles.copyLimited(
                new ByteArrayInputStream(source), output, new byte[4], source.length);

        assertEquals(source.length, copied);
        assertEquals("bedrock-pack", output.toString("UTF-8"));
    }

    @Test
    public void copyLimitedRejectsContentAboveLimit() {
        byte[] source = "0123456789".getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertThrows(IOException.class, () -> ContentFiles.copyLimited(
                new ByteArrayInputStream(source), output, new byte[8], 5));
        // The buffer that crosses the limit is rejected before any of its bytes are written.
        assertEquals(0, output.size());
    }

    @Test
    public void copyLimitedRejectsNegativeLimit() {
        assertThrows(IllegalArgumentException.class, () -> ContentFiles.copyLimited(
                new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), new byte[4], -1));
    }
}
