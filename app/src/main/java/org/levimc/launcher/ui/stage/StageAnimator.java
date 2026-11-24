package org.levimc.launcher.ui.stage;

import android.opengl.Matrix;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** Procedural idle, expression and secondary-motion controller for OBM rigs. */
final class StageAnimator {
    private static final int EXPR_NEUTRAL = 0;
    private static final int EXPR_SOFT_SMILE = 1;
    private static final int EXPR_HAPPY = 2;
    private static final int EXPR_SHY = 3;
    private static final int EXPR_CURIOUS = 4;
    private static final int EXPR_PLAYFUL = 5;

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
    private final int leftArm;
    private final int rightArm;
    private final int leftElbow;
    private final int rightElbow;
    private final int leftWrist;
    private final int rightWrist;
    private final int leftRingFinger;
    private final int rightRingFinger;
    private final int leftPinkyFinger;
    private final int rightPinkyFinger;
    private final int[] springs;
    private final int[] springSlot;
    private final float[] springAxisX;
    private final float[] springAxisY;
    private final float[] springAxisZ;
    private final float[] springRestAngle;
    private final float[] springInertia;
    private final float[] springAmplitude;
    private final float[] springPhase;
    private final float[] springAngle;
    private final float[] springVelocity;
    private final float[] tmpLocal = new float[16];
    private final float[] tmpRot = new float[16];
    private final float[] tmpA = new float[16];
    private final float[] tmpB = new float[16];
    private final Random random = new Random();

    private float time;
    private float lookX;
    private float lookY;
    private volatile float targetX;
    private volatile float targetY;
    private volatile float rotationTarget;
    private float bodyAngularVelocity;
    private float blinkTime = -1f;
    private float blinkStrength = 1f;
    private float nextBlink = 2.4f;
    private float reactTime = -1f;
    private volatile boolean reactRequested;
    private volatile int pendingEmote = -1;
    private int activeEmote = -1;
    private float emoteTime;
    private float emoteDuration;
    private int expression = EXPR_NEUTRAL;
    private float expressionTime;
    private float expressionDuration;
    private float idleExpressionTimer = 6.5f;
    private boolean expressionBlinkTriggered;
    private float expressionHeadRoll;
    private float expressionHeadPitch;
    private float expressionHeadYaw;
    private float expressionBodyLean;
    private String pendingSoundEvent;

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
        for (String name : pack.spineBones) {
            Integer idx = byName.get(name);
            if (idx != null) spineTmp[spineCount++] = idx;
        }
        spine = Arrays.copyOf(spineTmp, spineCount);
        Integer hipIndex = byName.get(pack.hipBone);
        Integer neckIndex = byName.get(pack.neckBone);
        Integer headIndex = byName.get(pack.headBone);
        Integer shoulderL = byName.get("Shoulder_L");
        Integer shoulderR = byName.get("Shoulder_R");
        Integer armL = byName.get("Arm_L");
        Integer armR = byName.get("Arm_R");
        Integer elbowL = byName.get("Elbow_L");
        Integer elbowR = byName.get("Elbow_R");
        Integer wristL = byName.get("Wrist_L");
        Integer wristR = byName.get("Wrist_R");
        Integer ringL = byName.get("Ring_01_L");
        Integer ringR = byName.get("Ring_01_R");
        Integer pinkyL = byName.get("Pinky_01_L");
        Integer pinkyR = byName.get("Pinky_01_R");
        hip = hipIndex == null ? -1 : hipIndex;
        neck = neckIndex == null ? -1 : neckIndex;
        head = headIndex == null ? -1 : headIndex;
        leftShoulder = shoulderL == null ? -1 : shoulderL;
        rightShoulder = shoulderR == null ? -1 : shoulderR;
        leftArm = armL == null ? -1 : armL;
        rightArm = armR == null ? -1 : armR;
        leftElbow = elbowL == null ? -1 : elbowL;
        rightElbow = elbowR == null ? -1 : elbowR;
        leftWrist = wristL == null ? -1 : wristL;
        rightWrist = wristR == null ? -1 : wristR;
        leftRingFinger = ringL == null ? -1 : ringL;
        rightRingFinger = ringR == null ? -1 : ringR;
        leftPinkyFinger = pinkyL == null ? -1 : pinkyL;
        rightPinkyFinger = pinkyR == null ? -1 : pinkyR;

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
        springAxisX = new float[count];
        springAxisY = new float[count];
        springAxisZ = new float[count];
        springRestAngle = new float[count];
        springInertia = new float[count];
        springAmplitude = new float[count];
        springPhase = new float[count];
        springAngle = new float[count];
        springVelocity = new float[count];
        buildSpringRig();

        smileWeight = clamp(pack.restSmile, 0f, 0.55f);
        idleExpressionTimer = 5.5f + random.nextFloat() * 3.5f;
    }

    private void buildSpringRig() {
        for (int s = 0; s < springs.length; s++) {
            int nodeIndex = springs[s];
            String name = model.nodes[nodeIndex].name.toLowerCase();
            float category;
            float gravityDegrees;
            float inertia;
            float amplitude;
            if (name.contains("tail")) {
                category = 1.45f;
                gravityDegrees = 11.5f;
                inertia = 0.075f;
                amplitude = 2.8f;
            } else if (name.contains("hair")) {
                category = 1.20f;
                gravityDegrees = 8.8f;
                inertia = 0.065f;
                amplitude = 2.0f;
            } else if (name.contains("skirt")) {
                category = 0.80f;
                gravityDegrees = 5.6f;
                inertia = 0.045f;
                amplitude = 1.1f;
            } else if (name.contains("ear")) {
                category = 0.65f;
                gravityDegrees = 4.3f;
                inertia = 0.055f;
                amplitude = 1.5f;
            } else if (name.contains("ribbon") || name.contains("scarf")) {
                category = 1.05f;
                gravityDegrees = 7.5f;
                inertia = 0.060f;
                amplitude = 1.8f;
            } else {
                category = 0.78f;
                gravityDegrees = 5.8f;
                inertia = 0.050f;
                amplitude = 1.2f;
            }

            // Find the first child segment and derive the rotation axis that
            // bends that segment toward world-down. This produces a real
            // gravity bias instead of the previous arbitrary Euler wobble.
            float dx = 0f;
            float dy = 0f;
            float dz = 0f;
            float bestLength = 0f;
            for (int child = 0; child < nodeCount; child++) {
                if (model.nodes[child].parent != nodeIndex) continue;
                float cx = model.nodes[child].local[12];
                float cy = model.nodes[child].local[13];
                float cz = model.nodes[child].local[14];
                float length = cx * cx + cy * cy + cz * cz;
                if (length > bestLength) {
                    bestLength = length;
                    dx = cx;
                    dy = cy;
                    dz = cz;
                }
            }
            float len = (float) Math.sqrt(bestLength);
            if (len > 0.0001f) {
                dx /= len;
                dy /= len;
                dz /= len;
            } else {
                dx = ((s & 1) == 0) ? 0.65f : -0.65f;
                dy = -0.35f;
                dz = 0.0f;
            }

            // axis = direction x gravity(0,-1,0), normalized.
            float ax = -dz;
            float ay = 0f;
            float az = dx;
            float axisLength = (float) Math.sqrt(ax * ax + az * az);
            if (axisLength < 0.08f) {
                ax = 0f;
                ay = 0f;
                az = ((s & 1) == 0) ? 1f : -1f;
                axisLength = 1f;
            }
            springAxisX[s] = ax / axisLength;
            springAxisY[s] = ay;
            springAxisZ[s] = az / axisLength;

            float horizontalAmount = clamp((float) Math.sqrt(dx * dx + dz * dz), 0.18f, 1f);
            float chainDepth = 0f;
            int parent = model.nodes[nodeIndex].parent;
            int guard = 0;
            while (parent >= 0 && springSlot[parent] >= 0 && guard++ < 8) {
                chainDepth += 0.025f;
                parent = model.nodes[parent].parent;
            }
            springRestAngle[s] = gravityDegrees * (0.42f + 0.58f * horizontalAmount)
                    * (1f + chainDepth) * pack.springs * pack.gravityScale;
            springInertia[s] = inertia * category * pack.springs;
            springAmplitude[s] = amplitude * category * pack.springs;
            springPhase[s] = random.nextFloat() * 6.2831853f;
        }
    }

    void setLook(float x, float y) {
        targetX = clamp(x, -1f, 1f);
        targetY = clamp(y, -1f, 1f);
    }

    void rotateBy(float degrees) {
        synchronized (this) {
            rotationTarget = clamp(rotationTarget + degrees, -72f, 72f);
        }
    }

    void react() {
        reactRequested = true;
    }

    /** Queue a one-shot skeletal pose. It eases in, holds briefly, then returns to idle. */
    void playEmote(int emoteId) {
        pendingEmote = clampInt(emoteId, 0, 3);
    }

    private void beginEmote(int emoteId) {
        activeEmote = emoteId;
        emoteTime = 0f;
        emoteDuration = emoteId == 3 ? 2.35f : 2.10f;
        triggerBlink(1f);
        if (emoteId == 0) startExpression(EXPR_HAPPY, emoteDuration, "wave");
        else if (emoteId == 1) startExpression(EXPR_PLAYFUL, emoteDuration, "peace");
        else if (emoteId == 2) startExpression(EXPR_HAPPY, emoteDuration, "cheer");
        else startExpression(EXPR_SOFT_SMILE, emoteDuration, "heart");
    }

    String consumeSoundEvent() {
        String event = pendingSoundEvent;
        pendingSoundEvent = null;
        return event;
    }

    float[] skinMatrices() {
        return skin;
    }

    int skinCount() {
        return model.skinCount;
    }

    void update(float dt) {
        dt = clamp(dt, 0.001f, 0.05f);
        time += dt;
        int requestedEmote = pendingEmote;
        if (requestedEmote >= 0) {
            pendingEmote = -1;
            beginEmote(requestedEmote);
        }
        if (activeEmote >= 0) {
            emoteTime += dt;
            if (emoteTime >= emoteDuration) activeEmote = -1;
        }
        float follow = 1f - (float) Math.exp(-dt * 6.0f);
        lookX += (targetX - lookX) * follow;
        lookY += (targetY - lookY) * follow;

        if (reactRequested) {
            reactRequested = false;
            reactTime = 0f;
            triggerBlink(1f);
            int picked = random.nextInt(5);
            if (picked == 0) startExpression(EXPR_HAPPY, 2.5f + random.nextFloat() * 0.8f, "happy");
            else if (picked == 1) startExpression(EXPR_SHY, 2.2f + random.nextFloat() * 0.8f, "shy");
            else if (picked == 2) startExpression(EXPR_CURIOUS, 2.4f + random.nextFloat() * 0.8f, "curious");
            else if (picked == 3) startExpression(EXPR_PLAYFUL, 2.6f + random.nextFloat() * 0.8f, "playful");
            else startExpression(EXPR_SOFT_SMILE, 2.8f + random.nextFloat() * 0.8f, "smile");
            pendingSoundEvent = "tap";
        }

        if (expression == EXPR_NEUTRAL) {
            idleExpressionTimer -= dt;
            if (idleExpressionTimer <= 0f) {
                int picked = random.nextInt(4);
                if (picked == 0) startExpression(EXPR_SOFT_SMILE, 2.8f + random.nextFloat() * 1.0f, "smile");
                else if (picked == 1) startExpression(EXPR_HAPPY, 2.3f + random.nextFloat() * 0.8f, "happy");
                else if (picked == 2) startExpression(EXPR_SHY, 2.5f + random.nextFloat() * 0.8f, "shy");
                else startExpression(EXPR_CURIOUS, 2.6f + random.nextFloat() * 0.8f, "curious");
            }
        } else {
            expressionTime += dt;
            if (expression == EXPR_SHY && !expressionBlinkTriggered && expressionTime > 0.48f) {
                triggerBlink(0.58f);
                expressionBlinkTriggered = true;
            }
            if (expressionTime >= expressionDuration) {
                expression = EXPR_NEUTRAL;
                expressionTime = 0f;
                expressionBlinkTriggered = false;
                idleExpressionTimer = 5.5f + random.nextFloat() * 5.5f;
            }
        }

        updateBlink(dt);
        if (reactTime >= 0f) {
            reactTime += dt;
            if (reactTime > 1.6f) reactTime = -1f;
        }

        float expressionEnvelope = expressionEnvelope();
        float smileTarget = pack.restSmile;
        float tiltTarget = 0f;
        float pitchTarget = 0f;
        float yawTarget = 0f;
        float leanTarget = 0f;
        switch (expression) {
            case EXPR_SOFT_SMILE:
                smileTarget = pack.softSmile;
                tiltTarget = -2.8f;
                pitchTarget = -0.8f;
                break;
            case EXPR_HAPPY:
                smileTarget = pack.happySmile;
                tiltTarget = 2.4f;
                pitchTarget = -1.4f;
                leanTarget = -0.8f;
                break;
            case EXPR_SHY:
                smileTarget = pack.shySmile;
                tiltTarget = -5.0f;
                pitchTarget = 2.3f;
                yawTarget = -2.0f;
                break;
            case EXPR_CURIOUS:
                smileTarget = pack.curiousSmile;
                tiltTarget = 5.0f;
                pitchTarget = -1.6f;
                yawTarget = 4.2f;
                break;
            case EXPR_PLAYFUL:
                smileTarget = pack.playfulSmile;
                tiltTarget = -4.0f;
                pitchTarget = -1.0f;
                yawTarget = -3.0f;
                leanTarget = 1.0f;
                break;
            default:
                break;
        }
        smileTarget = pack.restSmile + (smileTarget - pack.restSmile) * expressionEnvelope;
        tiltTarget *= expressionEnvelope;
        pitchTarget *= expressionEnvelope;
        yawTarget *= expressionEnvelope;
        leanTarget *= expressionEnvelope;
        float expressionFollow = 1f - (float) Math.exp(-dt * 5.0f);
        smileWeight += (clamp(smileTarget, 0f, 0.55f) - smileWeight) * expressionFollow;
        expressionHeadRoll += (tiltTarget - expressionHeadRoll) * expressionFollow;
        expressionHeadPitch += (pitchTarget - expressionHeadPitch) * expressionFollow;
        expressionHeadYaw += (yawTarget - expressionHeadYaw) * expressionFollow;
        expressionBodyLean += (leanTarget - expressionBodyLean) * expressionFollow;

        bobY = 0.0030f * (float) Math.sin(time * 1.05f);
        if (reactTime >= 0f) {
            bobY += 0.010f * (float) Math.exp(-reactTime * 2.8f)
                    * Math.abs((float) Math.sin(reactTime * 8.5f));
        }
        float desiredBodyYaw = rotationTarget + lookX * 5f * pack.lookAt
                + 0.16f * (float) Math.sin(time * 0.30f) * pack.sway
                + expressionBodyLean;
        float previousBodyYaw = bodyYaw;
        float bodyFollow = 1f - (float) Math.exp(-dt * 7.5f);
        bodyYaw += (desiredBodyYaw - bodyYaw) * bodyFollow;
        bodyAngularVelocity = (bodyYaw - previousBodyYaw) / dt;

        Arrays.fill(rotX, 0f);
        Arrays.fill(rotY, 0f);
        Arrays.fill(rotZ, 0f);
        float breath = (float) Math.sin(time * 6.2831853f * 0.22f) * pack.breath;
        if (hip >= 0) {
            rotY[hip] += 0.16f * (float) Math.sin(time * 0.42f) * pack.sway;
            rotZ[hip] += 0.10f * (float) Math.sin(time * 0.52f + 1.1f) * pack.sway;
        }
        for (int idx : spine) rotX[idx] += 0.78f * breath;
        if (spine.length > 0) {
            rotY[spine[0]] += 0.72f * (float) Math.sin(time * 0.48f) * pack.sway;
            rotZ[spine[0]] += 0.38f * (float) Math.sin(time * 0.66f + 1f) * pack.sway;
        }
        if (leftShoulder >= 0) {
            rotX[leftShoulder] += 0.20f * breath;
            rotZ[leftShoulder] += 0.18f * (float) Math.sin(time * 0.62f + 0.35f) * pack.sway;
        }
        if (rightShoulder >= 0) {
            rotX[rightShoulder] += 0.20f * breath;
            rotZ[rightShoulder] -= 0.18f * (float) Math.sin(time * 0.62f + 0.35f) * pack.sway;
        }

        applyEmotePose();

        float lookYaw = lookX * 14f * pack.lookAt;
        float lookPitch = -lookY * 7f * pack.lookAt;
        float idleHeadYaw = 0.38f * (float) Math.sin(time * 0.34f) * pack.sway;
        float idleHeadPitch = 0.18f * (float) Math.sin(time * 0.46f + 0.8f) * pack.sway;
        if (neck >= 0) {
            rotY[neck] += lookYaw * 0.4f + idleHeadYaw * 0.35f + expressionHeadYaw * 0.30f;
            rotX[neck] += lookPitch * 0.4f + idleHeadPitch * 0.35f + expressionHeadPitch * 0.28f;
            rotZ[neck] += expressionHeadRoll * 0.32f;
        }
        if (head >= 0) {
            rotY[head] += lookYaw * 0.6f + idleHeadYaw * 0.65f + expressionHeadYaw * 0.70f;
            rotX[head] += lookPitch * 0.6f + idleHeadPitch * 0.65f + expressionHeadPitch * 0.72f;
            rotZ[head] += expressionHeadRoll * 0.68f;
            if (reactTime >= 0f) rotZ[head] += 0.65f * (float) Math.sin(reactTime * 3.8f)
                    * (float) Math.exp(-reactTime * 1.4f);
        }

        for (int i = 0; i < nodeCount; i++) {
            ObmModel.Node node = model.nodes[i];
            System.arraycopy(node.local, 0, tmpLocal, 0, 16);
            int slot = springSlot[i];
            if (slot >= 0 && pack.springs > 0f) {
                float categoryWave = (float) Math.sin(time * 1.55f + springPhase[slot])
                        * springAmplitude[slot];
                float rotationLag = clamp(-bodyAngularVelocity * springInertia[slot], -17f, 17f);
                float targetAngle = springRestAngle[slot] + categoryWave + rotationLag;
                float stiffness = clamp(pack.springStiffness, 5f, 50f);
                float damping = clamp(pack.springDamping, 1f, 20f);
                // Small fixed substeps keep the spring stable at 24/30 FPS and
                // make it visibly lag, overshoot and settle after quick turns.
                float remaining = dt;
                while (remaining > 0f) {
                    float step = Math.min(remaining, 1f / 120f);
                    float acceleration = (targetAngle - springAngle[slot]) * stiffness
                            - springVelocity[slot] * damping;
                    springVelocity[slot] = clamp(springVelocity[slot] + acceleration * step, -95f, 95f);
                    springAngle[slot] = clamp(springAngle[slot] + springVelocity[slot] * step, -22f, 22f);
                    remaining -= step;
                }
                Matrix.setIdentityM(tmpRot, 0);
                Matrix.rotateM(tmpRot, 0, springAngle[slot],
                        springAxisX[slot], springAxisY[slot], springAxisZ[slot]);
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

    private void applyEmotePose() {
        if (activeEmote < 0) return;
        float fadeIn = smoothstep(0f, 0.22f, emoteTime);
        float fadeOut = 1f - smoothstep(emoteDuration - 0.34f, emoteDuration, emoteTime);
        float amount = clamp(fadeIn * fadeOut, 0f, 1f);
        if (amount <= 0f) return;

        // These presets target the humanoid shoulder/elbow/wrist joints. Models
        // without a given joint simply skip that part of the pose.
        switch (activeEmote) {
            case 0: // Wave: one arm raised with a small wrist wave.
                addRotationZ(rightShoulder, -82f * amount);
                addRotationZ(rightArm, -12f * amount);
                addRotationZ(rightElbow, 34f * amount);
                if (rightWrist >= 0) {
                    rotY[rightWrist] += (10f + 16f * (float) Math.sin(emoteTime * 10.5f)) * amount;
                    rotX[rightWrist] += -7f * amount;
                }
                break;
            case 1: // Double peace: both hands rise beside the face, with ring/pinky fingers softly curled.
                addRotationZ(leftShoulder, 78f * amount);
                addRotationZ(leftArm, 7f * amount);
                addRotationZ(leftElbow, -48f * amount);
                addRotationZ(rightShoulder, -78f * amount);
                addRotationZ(rightArm, -7f * amount);
                addRotationZ(rightElbow, 48f * amount);
                if (leftWrist >= 0) rotY[leftWrist] -= 14f * amount;
                if (rightWrist >= 0) rotY[rightWrist] += 14f * amount;
                addRotationZ(leftRingFinger, 24f * amount);
                addRotationZ(leftPinkyFinger, 34f * amount);
                addRotationZ(rightRingFinger, -24f * amount);
                addRotationZ(rightPinkyFinger, -34f * amount);
                break;
            case 2: // Cheer: both arms raised, elbows softly bent.
                addRotationZ(leftShoulder, 112f * amount);
                addRotationZ(leftArm, 10f * amount);
                addRotationZ(leftElbow, -26f * amount);
                addRotationZ(rightShoulder, -112f * amount);
                addRotationZ(rightArm, -10f * amount);
                addRotationZ(rightElbow, 26f * amount);
                if (leftWrist >= 0) rotX[leftWrist] += -8f * amount;
                if (rightWrist >= 0) rotX[rightWrist] += 8f * amount;
                break;
            case 3: // Heart: arms bend inward toward the chest.
                addRotationZ(leftShoulder, 46f * amount);
                addRotationZ(leftArm, -8f * amount);
                addRotationZ(leftElbow, -68f * amount);
                addRotationZ(rightShoulder, -46f * amount);
                addRotationZ(rightArm, 8f * amount);
                addRotationZ(rightElbow, 68f * amount);
                if (leftWrist >= 0) rotX[leftWrist] += 13f * amount;
                if (rightWrist >= 0) rotX[rightWrist] -= 13f * amount;
                break;
            default:
                break;
        }
    }

    private void addRotationZ(int node, float degrees) {
        if (node >= 0) rotZ[node] += degrees;
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private void startExpression(int next, float duration, String event) {
        expression = next;
        expressionTime = 0f;
        expressionDuration = duration;
        expressionBlinkTriggered = false;
        idleExpressionTimer = duration + 3.5f + random.nextFloat() * 4.5f;
        if (event != null && pendingSoundEvent == null) pendingSoundEvent = event;
    }

    private float expressionEnvelope() {
        if (expression == EXPR_NEUTRAL) return 0f;
        float fadeIn = smoothstep(0f, 0.28f, expressionTime);
        float fadeOut = 1f - smoothstep(expressionDuration - 0.48f, expressionDuration, expressionTime);
        return clamp(fadeIn * fadeOut, 0f, 1f);
    }

    private void triggerBlink(float strength) {
        if (!pack.blink) return;
        blinkTime = 0f;
        blinkStrength = clamp(strength, 0f, 1f);
        nextBlink = 2.5f + random.nextFloat() * 4.2f;
    }

    private void updateBlink(float dt) {
        if (!pack.blink) {
            blinkWeight = 0f;
            blinkTime = -1f;
            return;
        }
        if (blinkTime < 0f) {
            nextBlink -= dt;
            if (nextBlink <= 0f) triggerBlink(1f);
        }
        if (blinkTime >= 0f) {
            blinkTime += dt;
            float u = blinkTime / 0.24f;
            float envelope;
            if (u < 0.28f) envelope = u / 0.28f;
            else if (u < 0.43f) envelope = 1f;
            else envelope = Math.max(0f, 1f - (u - 0.43f) / 0.57f);
            blinkWeight = envelope * blinkStrength;
            if (u >= 1f) {
                blinkTime = -1f;
                blinkWeight = 0f;
            }
        }
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        if (edge1 <= edge0) return x >= edge1 ? 1f : 0f;
        float t = clamp((x - edge0) / (edge1 - edge0), 0f, 1f);
        return t * t * (3f - 2f * t);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
