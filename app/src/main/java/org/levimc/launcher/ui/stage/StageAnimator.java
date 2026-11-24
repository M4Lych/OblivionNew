package org.levimc.launcher.ui.stage;

import android.opengl.Matrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

final class StageAnimator {
    private final ObmModel model;
    private final ModelPack pack;
    private final int nodeCount;
    private final float[] globals;
    private final float[] skin;
    private final float[] rotX;
    private final float[] rotY;
    private final float[] rotZ;
    private final int[] spine;
    private final int hip;
    private final int neck;
    private final int head;
    private final int leftShoulder;
    private final int rightShoulder;
    private final int[] springs;
    private final float[] springCategory;
    private final float[] springGravityX;
    private final float[] springGravityZ;
    private final float[] springAngleX;
    private final float[] springAngleY;
    private final float[] springAngleZ;
    private final float[] springVelocityX;
    private final float[] springVelocityY;
    private final float[] springVelocityZ;
    private final int[] springSlot;
    private final float[] tmpLocal = new float[16];
    private final float[] tmpRot = new float[16];
    private final float[] tmpA = new float[16];
    private final float[] tmpB = new float[16];
    private final Random random = new Random(7);

    private float time;
    private float lookX;
    private float lookY;
    private volatile float targetX;
    private volatile float targetY;
    private volatile float rotationTarget;
    private float bodyAngularVelocity;
    private float blinkTime = -1f;
    private float nextBlink = 2.2f;
    private float reactTime = -1f;
    private volatile boolean reactRequested;

    float blinkWeight;
    float smileWeight;
    float bobY;
    float bodyYaw;

    StageAnimator(ObmModel model, ModelPack pack) {
        this.model = model;
        this.pack = pack;
        nodeCount = model.nodes.length;
        globals = new float[nodeCount * 16];
        skin = new float[Math.max(1, model.skinCount) * 16];
        for (int i = 0; i < model.skinCount; i++) Matrix.setIdentityM(skin, i * 16);
        rotX = new float[nodeCount];
        rotY = new float[nodeCount];
        rotZ = new float[nodeCount];
        Map<String, Integer> byName = new HashMap<>();
        for (int i = 0; i < nodeCount; i++) byName.put(model.nodes[i].name, i);
        int[] spineTmp = new int[pack.spineBones.length];
        int spineCount = 0;
        for (String s : pack.spineBones) {
            Integer idx = byName.get(s);
            if (idx != null) spineTmp[spineCount++] = idx;
        }
        spine = Arrays.copyOf(spineTmp, spineCount);
        Integer hipIndex = byName.get(pack.hipBone);
        Integer n = byName.get(pack.neckBone);
        Integer h = byName.get(pack.headBone);
        Integer shoulderL = byName.get("Shoulder_L");
        Integer shoulderR = byName.get("Shoulder_R");
        hip = hipIndex == null ? -1 : hipIndex;
        neck = n == null ? -1 : n;
        head = h == null ? -1 : h;
        leftShoulder = shoulderL == null ? -1 : shoulderL;
        rightShoulder = shoulderR == null ? -1 : shoulderR;
        springSlot = new int[nodeCount];
        Arrays.fill(springSlot, -1);
        int[] list = new int[nodeCount];
        int count = 0;
        for (int i = 0; i < nodeCount; i++) {
            String name = model.nodes[i].name;
            if (pack.springPrefix.length() > 0 && name.startsWith(pack.springPrefix)
                    && !name.contains("Handle") && !name.endsWith("_end")) {
                springSlot[i] = count;
                list[count++] = i;
            }
        }
        springs = Arrays.copyOf(list, count);
        springCategory = new float[count];
        springGravityX = new float[count];
        springGravityZ = new float[count];
        springAngleX = new float[count];
        springAngleY = new float[count];
        springAngleZ = new float[count];
        springVelocityX = new float[count];
        springVelocityY = new float[count];
        springVelocityZ = new float[count];
        for (int s = 0; s < count; s++) {
            int i = springs[s];
            String lower = model.nodes[i].name.toLowerCase();
            float category = 0.8f;
            if (lower.contains("tail")) category = 1.55f;
            else if (lower.contains("hair")) category = 1.15f;
            else if (lower.contains("skirt")) category = 0.82f;
            else if (lower.contains("ear")) category = 0.48f;
            else if (lower.contains("ribbon") || lower.contains("scarf")) category = 1.05f;
            int depth = 0;
            int parent = model.nodes[i].parent;
            while (parent >= 0 && springSlot[parent] >= 0 && depth < 12) {
                depth++;
                parent = model.nodes[parent].parent;
            }
            // A very small gravity bias makes hanging parts settle naturally.
            // Alternate the Z bias by bone so left/right strands do not move as
            // one rigid sheet. Runtime inertia is added when the body is rotated.
            float side = (s & 1) == 0 ? 1f : -1f;
            springCategory[s] = category * (1f + Math.min(depth, 5) * 0.06f);
            springGravityX[s] = lower.contains("tail") ? 0.38f : (lower.contains("hair") ? 0.22f : 0.10f);
            springGravityZ[s] = side * (lower.contains("hair") ? 0.14f : 0.08f) * category;
        }
    }

    void setLook(float x, float y) {
        targetX = x;
        targetY = y;
    }

    void rotateBy(float degrees) {
        synchronized (this) {
            rotationTarget = Math.max(-72f, Math.min(72f, rotationTarget + degrees));
        }
    }

    void react() {
        reactRequested = true;
    }

    float[] skinMatrices() {
        return skin;
    }

    int skinCount() {
        return model.skinCount;
    }

    void update(float dt) {
        dt = Math.max(0.001f, Math.min(0.05f, dt));
        time += dt;
        float follow = Math.min(1f, dt * 5f);
        lookX += (targetX - lookX) * follow;
        lookY += (targetY - lookY) * follow;
        if (reactRequested) {
            reactRequested = false;
            reactTime = 0f;
        }
        float react = 0f;
        if (reactTime >= 0f) {
            reactTime += dt;
            if (reactTime > 1.8f) reactTime = -1f;
            else react = (float) Math.sin(Math.PI * reactTime / 1.8f);
        }
        if (pack.blink) {
            nextBlink -= dt;
            if (blinkTime < 0f && nextBlink <= 0f) blinkTime = 0f;
            if (blinkTime >= 0f) {
                blinkTime += dt;
                float u = blinkTime / 0.26f;
                if (u < 0.35f) blinkWeight = u / 0.35f;
                else if (u < 0.5f) blinkWeight = 1f;
                else blinkWeight = Math.max(0f, 1f - (u - 0.5f) / 0.5f);
                if (u >= 1f) {
                    blinkTime = -1f;
                    blinkWeight = 0f;
                    nextBlink = 2.2f + random.nextFloat() * 3.5f;
                }
            }
        }
        smileWeight = 0f; // Keep the default resting mouth closed; avoid the source smile morph opening the mouth.
        bobY = 0.0025f * (float) Math.sin(time * 1.0f);
        if (reactTime >= 0f) bobY += 0.008f * (float) Math.exp(-reactTime * 3f) * Math.abs((float) Math.sin(reactTime * 9f));
        float desiredBodyYaw = rotationTarget + lookX * 5f * pack.lookAt
                + 0.12f * (float) Math.sin(time * 0.30f) * pack.sway;
        float previousBodyYaw = bodyYaw;
        float bodyFollow = 1f - (float) Math.exp(-dt * 8.5f);
        bodyYaw += (desiredBodyYaw - bodyYaw) * bodyFollow;
        bodyAngularVelocity = (bodyYaw - previousBodyYaw) / dt;

        Arrays.fill(rotX, 0f);
        Arrays.fill(rotY, 0f);
        Arrays.fill(rotZ, 0f);
        float breath = (float) Math.sin(time * 6.2831853f * 0.21f) * pack.breath;
        if (hip >= 0) {
            rotY[hip] += 0.12f * (float) Math.sin(time * 0.42f) * pack.sway;
            rotZ[hip] += 0.08f * (float) Math.sin(time * 0.52f + 1.1f) * pack.sway;
        }
        for (int idx : spine) rotX[idx] += 0.72f * breath;
        if (spine.length > 0) {
            rotY[spine[0]] += 0.65f * (float) Math.sin(time * 0.48f) * pack.sway;
            rotZ[spine[0]] += 0.35f * (float) Math.sin(time * 0.66f + 1f) * pack.sway;
        }
        if (leftShoulder >= 0) {
            rotX[leftShoulder] += 0.18f * breath;
            rotZ[leftShoulder] += 0.18f * (float) Math.sin(time * 0.62f + 0.35f) * pack.sway;
        }
        if (rightShoulder >= 0) {
            rotX[rightShoulder] += 0.18f * breath;
            rotZ[rightShoulder] -= 0.18f * (float) Math.sin(time * 0.62f + 0.35f) * pack.sway;
        }
        float yaw = lookX * 14f * pack.lookAt;
        float pitch = -lookY * 7f * pack.lookAt;
        float idleHeadYaw = 0.34f * (float) Math.sin(time * 0.34f) * pack.sway;
        float idleHeadPitch = 0.16f * (float) Math.sin(time * 0.46f + 0.8f) * pack.sway;
        if (neck >= 0) {
            rotY[neck] += yaw * 0.4f + idleHeadYaw * 0.35f;
            rotX[neck] += pitch * 0.4f + idleHeadPitch * 0.35f;
        }
        if (head >= 0) {
            rotY[head] += yaw * 0.6f + idleHeadYaw * 0.65f;
            rotX[head] += pitch * 0.6f + idleHeadPitch * 0.65f;
            if (reactTime >= 0f) rotZ[head] += 0.9f * react * (float) Math.sin(reactTime * 3.6f);
        }

        for (int i = 0; i < nodeCount; i++) {
            ObmModel.Node node = model.nodes[i];
            System.arraycopy(node.local, 0, tmpLocal, 0, 16);
            int slot = springSlot[i];
            if (slot >= 0 && pack.springs > 0f) {
                float category = springCategory[slot] * pack.springs;
                float targetX = springGravityX[slot] * category;
                // Rotational inertia: loose hair/accessories lag opposite to a
                // quick body turn, then settle with a damped spring response.
                float targetY = clamp(-bodyAngularVelocity * 0.020f * category, -7.0f, 7.0f);
                float targetZ = springGravityZ[slot] * pack.springs
                        + clamp(-bodyAngularVelocity * 0.008f * category, -3.0f, 3.0f);
                float stiffness = 23.0f;
                float damping = 8.2f;

                springVelocityX[slot] += ((targetX - springAngleX[slot]) * stiffness
                        - springVelocityX[slot] * damping) * dt;
                springVelocityY[slot] += ((targetY - springAngleY[slot]) * stiffness
                        - springVelocityY[slot] * damping) * dt;
                springVelocityZ[slot] += ((targetZ - springAngleZ[slot]) * stiffness
                        - springVelocityZ[slot] * damping) * dt;

                springVelocityX[slot] = clamp(springVelocityX[slot], -35f, 35f);
                springVelocityY[slot] = clamp(springVelocityY[slot], -55f, 55f);
                springVelocityZ[slot] = clamp(springVelocityZ[slot], -35f, 35f);
                springAngleX[slot] = clamp(springAngleX[slot] + springVelocityX[slot] * dt, -5f, 5f);
                springAngleY[slot] = clamp(springAngleY[slot] + springVelocityY[slot] * dt, -8f, 8f);
                springAngleZ[slot] = clamp(springAngleZ[slot] + springVelocityZ[slot] * dt, -5f, 5f);

                Matrix.setIdentityM(tmpRot, 0);
                Matrix.rotateM(tmpRot, 0, springAngleX[slot], 1f, 0f, 0f);
                Matrix.rotateM(tmpRot, 0, springAngleY[slot], 0f, 1f, 0f);
                Matrix.rotateM(tmpRot, 0, springAngleZ[slot], 0f, 0f, 1f);
                Matrix.multiplyMM(tmpA, 0, node.local, 0, tmpRot, 0);
                System.arraycopy(tmpA, 0, tmpLocal, 0, 16);
            }
            if (node.parent < 0) {
                System.arraycopy(tmpLocal, 0, globals, i * 16, 16);
            } else {
                Matrix.multiplyMM(globals, i * 16, globals, node.parent * 16, tmpLocal, 0);
            }
            if (rotX[i] != 0f || rotY[i] != 0f || rotZ[i] != 0f) {
                float ox = globals[i * 16 + 12];
                float oy = globals[i * 16 + 13];
                float oz = globals[i * 16 + 14];
                Matrix.setIdentityM(tmpA, 0);
                Matrix.translateM(tmpA, 0, ox, oy, oz);
                Matrix.rotateM(tmpA, 0, rotY[i], 0f, 1f, 0f);
                Matrix.rotateM(tmpA, 0, rotX[i], 1f, 0f, 0f);
                Matrix.rotateM(tmpA, 0, rotZ[i], 0f, 0f, 1f);
                Matrix.translateM(tmpA, 0, -ox, -oy, -oz);
                System.arraycopy(globals, i * 16, tmpB, 0, 16);
                Matrix.multiplyMM(globals, i * 16, tmpA, 0, tmpB, 0);
            }
        }
        for (int i = 0; i < nodeCount; i++) {
            ObmModel.Node node = model.nodes[i];
            if (node.skin >= 0 && node.skin < model.skinCount) {
                System.arraycopy(globals, i * 16, tmpB, 0, 16);
                Matrix.multiplyMM(skin, node.skin * 16, tmpB, 0, node.inverseBind, 0);
            }
        }
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

}
