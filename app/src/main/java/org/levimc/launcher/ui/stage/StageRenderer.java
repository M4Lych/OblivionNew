package org.levimc.launcher.ui.stage;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.GLES30;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.Map;

final class StageRenderer {
    private static final String TAG = "OblivionStage";

    private static final String VERTEX =
            "#version 300 es\n"
                    + "layout(location=0) in vec3 aPos;\n"
                    + "layout(location=1) in vec3 aNormal;\n"
                    + "layout(location=2) in vec2 aUv;\n"
                    + "layout(location=3) in ivec4 aBoneIdx;\n"
                    + "layout(location=4) in vec4 aBoneW;\n"
                    + "layout(location=5) in vec3 aBlink;\n"
                    + "layout(location=6) in vec3 aSmile;\n"
                    + "uniform mat4 uWorld;\n"
                    + "uniform mat4 uViewProj;\n"
                    + "uniform highp sampler2D uBones;\n"
                    + "uniform float uBlink;\n"
                    + "uniform float uSmile;\n"
                    + "uniform highp float uOutline;\n"
                    + "out vec3 vN;\n"
                    + "out vec2 vUv;\n"
                    + "out vec3 vPos;\n"
                    + "mat4 bone(int i) {\n"
                    + "  return mat4(texelFetch(uBones, ivec2(0, i), 0), texelFetch(uBones, ivec2(1, i), 0),\n"
                    + "              texelFetch(uBones, ivec2(2, i), 0), texelFetch(uBones, ivec2(3, i), 0));\n"
                    + "}\n"
                    + "void main() {\n"
                    + "  vec3 p = aPos + aBlink * uBlink + aSmile * uSmile;\n"
                    + "  float total = aBoneW.x + aBoneW.y + aBoneW.z + aBoneW.w;\n"
                    + "  mat4 skin = mat4(1.0);\n"
                    + "  if (total > 0.001) {\n"
                    + "    skin = aBoneW.x * bone(aBoneIdx.x) + aBoneW.y * bone(aBoneIdx.y)\n"
                    + "         + aBoneW.z * bone(aBoneIdx.z) + aBoneW.w * bone(aBoneIdx.w);\n"
                    + "  }\n"
                    + "  vec4 sp = skin * vec4(p, 1.0);\n"
                    + "  vec3 sn = mat3(skin) * aNormal;\n"
                    + "  vec4 wp = uWorld * sp;\n"
                    + "  vec3 wn = normalize(mat3(uWorld) * sn);\n"
                    + "  if (uOutline > 0.0) wp.xyz += wn * uOutline;\n"
                    + "  vPos = wp.xyz;\n"
                    + "  vN = wn;\n"
                    + "  vUv = aUv;\n"
                    + "  gl_Position = uViewProj * wp;\n"
                    + "}\n";

    private static final String FRAGMENT =
            "#version 300 es\n"
                    + "precision mediump float;\n"
                    + "in vec3 vN;\n"
                    + "in vec2 vUv;\n"
                    + "in vec3 vPos;\n"
                    + "uniform sampler2D uDiffuse;\n"
                    + "uniform sampler2D uShade;\n"
                    + "uniform sampler2D uEmissive;\n"
                    + "uniform int uHasShade;\n"
                    + "uniform int uHasEmissive;\n"
                    + "uniform float uAlphaCut;\n"
                    + "uniform highp float uOutline;\n"
                    + "uniform vec3 uLightDir;\n"
                    + "uniform vec3 uCamPos;\n"
                    + "uniform vec3 uRimColor;\n"
                    + "uniform vec3 uOutlineColor;\n"
                    + "out vec4 oColor;\n"
                    + "void main() {\n"
                    + "  vec4 base = texture(uDiffuse, vUv);\n"
                    + "  if (base.a < uAlphaCut) discard;\n"
                    + "  if (uOutline > 0.0) {\n"
                    + "    oColor = vec4(uOutlineColor, 1.0);\n"
                    + "    return;\n"
                    + "  }\n"
                    + "  vec3 n = normalize(vN);\n"
                    + "  if (!gl_FrontFacing) n = -n;\n"
                    + "  float ndl = dot(n, normalize(uLightDir)) * 0.5 + 0.5;\n"
                    + "  float lit = smoothstep(0.40, 0.60, ndl);\n"
                    + "  vec3 shade = uHasShade == 1 ? texture(uShade, vUv).rgb : base.rgb * vec3(0.72, 0.66, 0.84);\n"
                    + "  vec3 col = mix(shade, base.rgb, lit);\n"
                    + "  vec3 v = normalize(uCamPos - vPos);\n"
                    + "  float rim = pow(1.0 - clamp(dot(n, v), 0.0, 1.0), 3.0);\n"
                    + "  col += uRimColor * rim * 0.55;\n"
                    + "  if (uHasEmissive == 1) col += texture(uEmissive, vUv).rgb * 0.6;\n"
                    + "  oColor = vec4(col, 1.0);\n"
                    + "}\n";

    private final ObmModel model;
    private final ModelPack pack;
    private int program;
    private int vao;
    private final int[] buffers = new int[8];
    private int boneTexture;
    private FloatBuffer boneBuffer;
    private int blinkIndex;
    private int smileIndex;
    private final Map<String, Integer> textures = new HashMap<>();
    private int whiteTexture;

    private int uWorld;
    private int uViewProj;
    private int uBones;
    private int uBlink;
    private int uSmile;
    private int uOutline;
    private int uDiffuse;
    private int uShade;
    private int uEmissive;
    private int uHasShade;
    private int uHasEmissive;
    private int uAlphaCut;
    private int uLightDir;
    private int uCamPos;
    private int uRimColor;
    private int uOutlineColor;

    private final float[] proj = new float[16];
    private final float[] view = new float[16];
    private final float[] viewProj = new float[16];
    private final float[] shift = new float[16];
    private final float[] shiftedViewProj = new float[16];
    private final float[] worldTmp = new float[16];
    private final float[] worldFinal = new float[16];

    StageRenderer(ObmModel model, ModelPack pack) {
        this.model = model;
        this.pack = pack;
    }

    private static int compile(int type, String source) {
        int shader = GLES30.glCreateShader(type);
        GLES30.glShaderSource(shader, source);
        GLES30.glCompileShader(shader);
        int[] ok = new int[1];
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            String log = GLES30.glGetShaderInfoLog(shader);
            GLES30.glDeleteShader(shader);
            throw new IllegalStateException("shader compile failed: " + log);
        }
        return shader;
    }

    private static FloatBuffer floatBuffer(float[] data) {
        FloatBuffer fb = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        fb.put(data).position(0);
        return fb;
    }

    private static ShortBuffer shortBuffer(short[] data) {
        ShortBuffer sb = ByteBuffer.allocateDirect(data.length * 2).order(ByteOrder.nativeOrder()).asShortBuffer();
        sb.put(data).position(0);
        return sb;
    }

    private int loadTexture(String path) {
        if (path == null) return 0;
        Integer cached = textures.get(path);
        if (cached != null) return cached;
        int id = 0;
        try (InputStream in = pack.source.open(path)) {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            Bitmap bmp = BitmapFactory.decodeStream(in, null, opts);
            if (bmp != null) {
                int[] t = new int[1];
                GLES30.glGenTextures(1, t, 0);
                id = t[0];
                GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, id);
                GLUtils.texImage2D(GLES30.GL_TEXTURE_2D, 0, bmp, 0);
                GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT);
                GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT);
                bmp.recycle();
            }
        } catch (IOException e) {
            Log.w(TAG, "texture missing: " + path);
        }
        textures.put(path, id);
        return id;
    }

    void init() {
        int vs = compile(GLES30.GL_VERTEX_SHADER, VERTEX);
        int fs = compile(GLES30.GL_FRAGMENT_SHADER, FRAGMENT);
        program = GLES30.glCreateProgram();
        GLES30.glAttachShader(program, vs);
        GLES30.glAttachShader(program, fs);
        GLES30.glLinkProgram(program);
        int[] ok = new int[1];
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, ok, 0);
        if (ok[0] == 0) throw new IllegalStateException("program link failed: " + GLES30.glGetProgramInfoLog(program));
        GLES30.glDeleteShader(vs);
        GLES30.glDeleteShader(fs);

        uWorld = GLES30.glGetUniformLocation(program, "uWorld");
        uViewProj = GLES30.glGetUniformLocation(program, "uViewProj");
        uBones = GLES30.glGetUniformLocation(program, "uBones");
        uBlink = GLES30.glGetUniformLocation(program, "uBlink");
        uSmile = GLES30.glGetUniformLocation(program, "uSmile");
        uOutline = GLES30.glGetUniformLocation(program, "uOutline");
        uDiffuse = GLES30.glGetUniformLocation(program, "uDiffuse");
        uShade = GLES30.glGetUniformLocation(program, "uShade");
        uEmissive = GLES30.glGetUniformLocation(program, "uEmissive");
        uHasShade = GLES30.glGetUniformLocation(program, "uHasShade");
        uHasEmissive = GLES30.glGetUniformLocation(program, "uHasEmissive");
        uAlphaCut = GLES30.glGetUniformLocation(program, "uAlphaCut");
        uLightDir = GLES30.glGetUniformLocation(program, "uLightDir");
        uCamPos = GLES30.glGetUniformLocation(program, "uCamPos");
        uRimColor = GLES30.glGetUniformLocation(program, "uRimColor");
        uOutlineColor = GLES30.glGetUniformLocation(program, "uOutlineColor");

        int[] vaos = new int[1];
        GLES30.glGenVertexArrays(1, vaos, 0);
        vao = vaos[0];
        GLES30.glBindVertexArray(vao);
        GLES30.glGenBuffers(buffers.length, buffers, 0);
        int vc = model.vertexCount;

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[0]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 12, floatBuffer(model.positions), GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(0);
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[1]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 12, floatBuffer(model.normals), GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(1);
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[2]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 8, floatBuffer(model.uvs), GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(2);
        GLES30.glVertexAttribPointer(2, 2, GLES30.GL_FLOAT, false, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[3]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 8, shortBuffer(model.boneIndices), GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(3);
        GLES30.glVertexAttribIPointer(3, 4, GLES30.GL_UNSIGNED_SHORT, 0, 0);

        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[4]);
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 16, floatBuffer(model.weights), GLES30.GL_STATIC_DRAW);
        GLES30.glEnableVertexAttribArray(4);
        GLES30.glVertexAttribPointer(4, 4, GLES30.GL_FLOAT, false, 0, 0);

        float[] blink = model.morphs.get(pack.blinkMorph);
        if (blink != null && blink.length == vc * 3) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[5]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 12, floatBuffer(blink), GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(5);
            GLES30.glVertexAttribPointer(5, 3, GLES30.GL_FLOAT, false, 0, 0);
            blinkIndex = 1;
        } else {
            GLES30.glDisableVertexAttribArray(5);
            GLES30.glVertexAttrib3f(5, 0f, 0f, 0f);
        }
        float[] smile = model.morphs.get(pack.smileMorph);
        if (smile != null && smile.length == vc * 3) {
            GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffers[6]);
            GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vc * 12, floatBuffer(smile), GLES30.GL_STATIC_DRAW);
            GLES30.glEnableVertexAttribArray(6);
            GLES30.glVertexAttribPointer(6, 3, GLES30.GL_FLOAT, false, 0, 0);
            smileIndex = 1;
        } else {
            GLES30.glDisableVertexAttribArray(6);
            GLES30.glVertexAttrib3f(6, 0f, 0f, 0f);
        }

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffers[7]);
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, model.indexCount * 2, shortBuffer(model.indices), GLES30.GL_STATIC_DRAW);
        GLES30.glBindVertexArray(0);

        int rows = Math.max(1, model.skinCount);
        int[] tex = new int[2];
        GLES30.glGenTextures(2, tex, 0);
        boneTexture = tex[0];
        whiteTexture = tex[1];
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, boneTexture);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA32F, 4, rows, 0, GLES30.GL_RGBA, GLES30.GL_FLOAT, null);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE);
        boneBuffer = ByteBuffer.allocateDirect(rows * 64).order(ByteOrder.nativeOrder()).asFloatBuffer();

        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, whiteTexture);
        ByteBuffer white = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());
        white.put((byte) 255).put((byte) 255).put((byte) 255).put((byte) 255).position(0);
        GLES30.glTexImage2D(GLES30.GL_TEXTURE_2D, 0, GLES30.GL_RGBA, 1, 1, 0, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, white);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_NEAREST);
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_NEAREST);

        for (ModelPack.Material m : pack.materials) {
            loadTexture(m.diffuse);
            loadTexture(m.shade);
            loadTexture(m.emissive);
        }
    }

    private int textureOf(String path) {
        if (path == null) return whiteTexture;
        Integer id = textures.get(path);
        return id == null || id == 0 ? whiteTexture : id;
    }

    private boolean hasTexture(String path) {
        if (path == null) return false;
        Integer id = textures.get(path);
        return id != null && id != 0;
    }

    private void drawSubmeshes(boolean outlinePass) {
        for (ObmModel.Submesh sm : model.submeshes) {
            if (sm.material < 0 || sm.material >= pack.materials.length) continue;
            ModelPack.Material m = pack.materials[sm.material];
            if (outlinePass && (!m.outline || m.alpha)) continue;
            GLES30.glActiveTexture(GLES30.GL_TEXTURE0);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureOf(m.diffuse));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE1);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureOf(m.shade));
            GLES30.glActiveTexture(GLES30.GL_TEXTURE2);
            GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureOf(m.emissive));
            GLES30.glUniform1i(uHasShade, hasTexture(m.shade) ? 1 : 0);
            GLES30.glUniform1i(uHasEmissive, hasTexture(m.emissive) ? 1 : 0);
            GLES30.glUniform1f(uAlphaCut, m.alpha ? 0.5f : 0f);
            if (!outlinePass) {
                if (m.alpha) GLES30.glDisable(GLES30.GL_CULL_FACE);
                else GLES30.glEnable(GLES30.GL_CULL_FACE);
            }
            GLES30.glDrawElements(GLES30.GL_TRIANGLES, sm.count, GLES30.GL_UNSIGNED_SHORT, sm.start * 2);
        }
    }

    void draw(StageAnimator animator, int width, int height) {
        GLES30.glViewport(0, 0, width, height);
        GLES30.glClearColor(0f, 0f, 0f, 0f);
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT | GLES30.GL_DEPTH_BUFFER_BIT);
        GLES30.glEnable(GLES30.GL_DEPTH_TEST);
        GLES30.glDisable(GLES30.GL_BLEND);

        GLES30.glUseProgram(program);

        float[] skin = animator.skinMatrices();
        int rows = Math.max(1, model.skinCount);
        boneBuffer.position(0);
        boneBuffer.put(skin, 0, rows * 16);
        boneBuffer.position(0);
        GLES30.glActiveTexture(GLES30.GL_TEXTURE3);
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, boneTexture);
        GLES30.glTexSubImage2D(GLES30.GL_TEXTURE_2D, 0, 0, 0, 4, rows, GLES30.GL_RGBA, GLES30.GL_FLOAT, boneBuffer);
        GLES30.glUniform1i(uBones, 3);
        GLES30.glUniform1i(uDiffuse, 0);
        GLES30.glUniform1i(uShade, 1);
        GLES30.glUniform1i(uEmissive, 2);

        float aspect = width / (float) Math.max(1, height);
        Matrix.perspectiveM(proj, 0, pack.fov, aspect, 0.1f, 50f);
        Matrix.setLookAtM(view, 0, 0f, pack.targetY, pack.distance, 0f, pack.targetY, 0f, 0f, 1f, 0f);
        Matrix.multiplyMM(viewProj, 0, proj, 0, view, 0);
        Matrix.setIdentityM(shift, 0);
        shift[12] = pack.offsetX;
        Matrix.multiplyMM(shiftedViewProj, 0, shift, 0, viewProj, 0);
        GLES30.glUniformMatrix4fv(uViewProj, 1, false, shiftedViewProj, 0);

        Matrix.setIdentityM(worldTmp, 0);
        Matrix.translateM(worldTmp, 0, 0f, animator.bobY, 0f);
        Matrix.rotateM(worldTmp, 0, animator.bodyYaw, 0f, 1f, 0f);
        Matrix.multiplyMM(worldFinal, 0, worldTmp, 0, model.world, 0);
        GLES30.glUniformMatrix4fv(uWorld, 1, false, worldFinal, 0);

        GLES30.glUniform1f(uBlink, blinkIndex == 1 ? animator.blinkWeight : 0f);
        GLES30.glUniform1f(uSmile, smileIndex == 1 ? animator.smileWeight : 0f);
        GLES30.glUniform3f(uLightDir, -0.35f, 0.55f, 0.75f);
        GLES30.glUniform3f(uCamPos, 0f, pack.targetY, pack.distance);
        GLES30.glUniform3f(uRimColor, 0.86f, 0.17f, 0.35f);
        GLES30.glUniform3f(uOutlineColor, 0.10f, 0.03f, 0.06f);

        GLES30.glBindVertexArray(vao);

        GLES30.glUniform1f(uOutline, 0.006f);
        GLES30.glEnable(GLES30.GL_CULL_FACE);
        GLES30.glCullFace(GLES30.GL_FRONT);
        drawSubmeshes(true);

        GLES30.glUniform1f(uOutline, 0f);
        GLES30.glCullFace(GLES30.GL_BACK);
        drawSubmeshes(false);

        GLES30.glBindVertexArray(0);
    }

    void release() {
        if (program != 0) GLES30.glDeleteProgram(program);
        GLES30.glDeleteBuffers(buffers.length, buffers, 0);
        if (vao != 0) GLES30.glDeleteVertexArrays(1, new int[]{vao}, 0);
        if (boneTexture != 0) GLES30.glDeleteTextures(1, new int[]{boneTexture}, 0);
        if (whiteTexture != 0) GLES30.glDeleteTextures(1, new int[]{whiteTexture}, 0);
        for (int id : textures.values()) {
            if (id != 0) GLES30.glDeleteTextures(1, new int[]{id}, 0);
        }
        textures.clear();
        program = 0;
        vao = 0;
    }
}
