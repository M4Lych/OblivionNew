package org.levimc.launcher.ui.stage;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class ObmModel {
    static final class Node {
        int parent;
        int skin;
        String name;
        float[] local = new float[16];
        float[] inverseBind = new float[16];
    }

    static final class Submesh {
        int start;
        int count;
        int material;
    }

    int vertexCount;
    int indexCount;
    int skinCount;
    float[] world = new float[16];
    float[] positions;
    float[] normals;
    float[] uvs;
    float[] weights;
    short[] boneIndices;
    short[] indices;
    Submesh[] submeshes;
    Node[] nodes;
    final Map<String, float[]> morphs = new LinkedHashMap<>();

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(1 << 16);
        byte[] buffer = new byte[1 << 16];
        int read;
        while ((read = in.read(buffer)) > 0) out.write(buffer, 0, read);
        in.close();
        return out.toByteArray();
    }

    private static float[] floats(ByteBuffer b, int count) {
        float[] out = new float[count];
        for (int i = 0; i < count; i++) out[i] = b.getFloat();
        return out;
    }

    static ObmModel parse(byte[] data) throws IOException {
        ByteBuffer b = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        if (data.length < 100 || b.getInt() != 0x314D424F) throw new IOException("not an OBM model");
        ObmModel m = new ObmModel();
        m.vertexCount = b.getInt();
        m.indexCount = b.getInt();
        m.skinCount = b.getInt();
        int nodeCount = b.getInt();
        int submeshCount = b.getInt();
        int morphCount = b.getInt();
        b.getInt();
        if (m.vertexCount <= 0 || m.vertexCount > 65535 || m.indexCount <= 0 || m.indexCount % 3 != 0
                || m.skinCount < 0 || nodeCount < 0 || submeshCount < 0 || morphCount < 0) {
            throw new IOException("invalid OBM header");
        }
        for (int i = 0; i < 16; i++) m.world[i] = b.getFloat();
        int vc = m.vertexCount;
        m.positions = floats(b, vc * 3);
        m.normals = floats(b, vc * 3);
        m.uvs = floats(b, vc * 2);
        m.boneIndices = new short[vc * 4];
        for (int i = 0; i < vc * 4; i++) m.boneIndices[i] = b.getShort();
        m.weights = floats(b, vc * 4);
        m.indices = new short[m.indexCount];
        for (int i = 0; i < m.indexCount; i++) m.indices[i] = b.getShort();
        m.submeshes = new Submesh[submeshCount];
        for (int i = 0; i < submeshCount; i++) {
            Submesh s = new Submesh();
            s.start = b.getInt();
            s.count = b.getInt();
            s.material = b.getInt();
            m.submeshes[i] = s;
        }
        m.nodes = new Node[nodeCount];
        for (int i = 0; i < nodeCount; i++) {
            Node n = new Node();
            n.parent = b.getInt();
            int nameLength = b.getShort() & 0xFFFF;
            n.skin = b.getInt();
            byte[] name = new byte[nameLength];
            b.get(name);
            n.name = new String(name, StandardCharsets.UTF_8);
            for (int k = 0; k < 16; k++) n.local[k] = b.getFloat();
            for (int k = 0; k < 16; k++) n.inverseBind[k] = b.getFloat();
            m.nodes[i] = n;
        }
        for (int i = 0; i < morphCount; i++) {
            int nameLength = b.getShort() & 0xFFFF;
            byte[] name = new byte[nameLength];
            b.get(name);
            m.morphs.put(new String(name, StandardCharsets.UTF_8), floats(b, vc * 3));
        }
        return m;
    }
}
