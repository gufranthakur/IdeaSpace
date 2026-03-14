package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.ideaspace.IdeaSpace;

public class AnimationHandler {

    private IdeaSpace ideaSpace;

    // -- Remove animation --
    private boolean isAnimating = false;
    private ModelInstance animatingModel;
    private float animationTime;
    private Runnable onComplete;
    private Matrix4 removeOriginalTransform = new Matrix4();

    public float POP_DURATION      = 0.12f;
    public float COLLAPSE_DURATION = 0.22f;
    public float POP_SCALE         = 1.30f;

    // -- Spawn animation --
    private boolean isSpawnAnimating = false;
    private ModelInstance spawnModel;
    private float spawnTime;
    private Matrix4 spawnOriginalTransform = new Matrix4();

    public float SPAWN_GROW_DURATION   = 0.18f;
    public float SPAWN_SETTLE_DURATION = 0.12f;
    public float SPAWN_OVERSHOOT_SCALE = 1.20f;

    public AnimationHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    public void spawnModelAnimation(ModelInstance modelInstance) {
        if (isSpawnAnimating) return;

        spawnModel = modelInstance;
        isSpawnAnimating = true;
        spawnTime = 0f;

        spawnOriginalTransform.set(spawnModel.transform);
        applyScaleTo(spawnModel, spawnOriginalTransform, 0f); // start invisible
    }

    public void removeModelAnimation(ModelInstance modelInstance, Runnable onComplete) {
        if (isAnimating) {
            System.out.println("Animation already in progress!");
            return;
        }

        animatingModel = modelInstance;
        this.onComplete = onComplete;
        isAnimating = true;
        animationTime = 0f;

        removeOriginalTransform.set(animatingModel.transform);
    }

    // -------------------------------------------------------------------------
    // Update — call every frame
    // -------------------------------------------------------------------------

    public void update() {
        updateSpawn();
        updateRemove();
    }

    // -------------------------------------------------------------------------
    // Internal
    // -------------------------------------------------------------------------

    private void updateSpawn() {
        if (!isSpawnAnimating || spawnModel == null) return;

        spawnTime += Gdx.graphics.getDeltaTime();
        float total = SPAWN_GROW_DURATION + SPAWN_SETTLE_DURATION;
        float scale;

        if (spawnTime < SPAWN_GROW_DURATION) {
            // Phase 1: 0 → SPAWN_OVERSHOOT_SCALE  (easeOutQuart — fast burst)
            float t = spawnTime / SPAWN_GROW_DURATION;
            float eased = 1f - (1f - t) * (1f - t) * (1f - t) * (1f - t);
            scale = SPAWN_OVERSHOOT_SCALE * eased;

        } else if (spawnTime < total) {
            // Phase 2: SPAWN_OVERSHOOT_SCALE → 1.0  (easeOutQuad — soft settle)
            float t = (spawnTime - SPAWN_GROW_DURATION) / SPAWN_SETTLE_DURATION;
            float eased = 1f - (1f - t) * (1f - t);
            scale = SPAWN_OVERSHOOT_SCALE + (1f - SPAWN_OVERSHOOT_SCALE) * eased;

        } else {
            applyScaleTo(spawnModel, spawnOriginalTransform, 1f);
            isSpawnAnimating = false;
            spawnModel = null;
            spawnTime = 0f;
            return;
        }

        applyScaleTo(spawnModel, spawnOriginalTransform, scale);
    }

    private void updateRemove() {
        if (!isAnimating || animatingModel == null) return;

        animationTime += Gdx.graphics.getDeltaTime();
        float total = POP_DURATION + COLLAPSE_DURATION;
        float scale;

        if (animationTime < POP_DURATION) {
            // Phase 1: 1.0 → POP_SCALE  (easeOutQuad — snaps to peak)
            float t = animationTime / POP_DURATION;
            float eased = 1f - (1f - t) * (1f - t);
            scale = 1f + (POP_SCALE - 1f) * eased;

        } else if (animationTime < total) {
            // Phase 2: POP_SCALE → 0  (easeInQuart — hard implosion)
            float t = (animationTime - POP_DURATION) / COLLAPSE_DURATION;
            float eased = t * t * t * t;
            scale = POP_SCALE * (1f - eased);

        } else {
            isAnimating = false;
            animatingModel = null;
            animationTime = 0f;
            if (onComplete != null) onComplete.run();
            return;
        }

        applyScaleTo(animatingModel, removeOriginalTransform, scale);
    }

    /** Rebuilds transform from snapshot then applies uniform scale — no drift. */
    private void applyScaleTo(ModelInstance target, Matrix4 originalTransform, float scale) {
        target.transform.set(originalTransform).scale(scale, scale, scale);
    }

    public boolean isAnimating()      { return isAnimating; }
    public boolean isSpawnAnimating() { return isSpawnAnimating; }
}
