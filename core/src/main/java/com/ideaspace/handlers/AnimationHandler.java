package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.ideaspace.IdeaSpace;

public class AnimationHandler {

    private IdeaSpace ideaSpace;
    private boolean isAnimating = false;
    private ModelInstance animatingModel;
    private Vector3 direction;
    private Vector3 startPosition;
    private float animationTime;
    private Runnable onComplete;

    // Configurable settings for testing
    public float REMOVE_ANIMATION_DURATION = 2f; // Duration in seconds
    public float REMOVE_ANIMATION_DISTANCE = 70f;  // Distance to travel
    public float REMOVE_ANIMATION_ROTATION_SPEED = 0f; // Degrees per second
    public float REMOVE_ANIMATION_SPEED_SCALE = 20f; // Speed multiplier (1.0 = normal, higher = faster)

    public AnimationHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        this.direction = new Vector3();
        this.startPosition = new Vector3();
    }

    public void removeModelAnimation(ModelInstance modelInstance, Runnable onComplete) {
        if (isAnimating) {
            System.out.println("Animation already in progress!");
            return;
        }

        this.animatingModel = modelInstance;
        this.onComplete = onComplete;
        this.isAnimating = true;
        this.animationTime = 0f;

        // Store starting position
        startPosition.set(animatingModel.transform.getTranslation(new Vector3()));

        // Get camera direction
        Camera camera = ideaSpace.space.camera;
        direction.set(camera.direction).nor();
    }

    public void update() {
        if (!isAnimating || animatingModel == null) return;

        float delta = Gdx.graphics.getDeltaTime();
        animationTime += delta;

        // Calculate progress (0 to 1)
        float progress = Math.min(animationTime / REMOVE_ANIMATION_DURATION, 1f);

        // Ease out effect for smoother animation
        float eased = 1f - (1f - progress) * (1f - progress);

        // Calculate speed based on distance and duration, with speed scaling
        float speed = (REMOVE_ANIMATION_DISTANCE / REMOVE_ANIMATION_DURATION) * REMOVE_ANIMATION_SPEED_SCALE;

        // Move model in camera direction
        animatingModel.transform.translate(
            direction.x * speed * delta,
            direction.y * speed * delta,
            direction.z * speed * delta
        );

        // Rotate the model around its own center
        // Create rotation axis perpendicular to direction (for tumbling effect)
        Vector3 rotationAxis = new Vector3(direction).crs(Vector3.Y).nor();
        if (rotationAxis.len() < 0.1f) { // If direction is parallel to Y, use X axis
            rotationAxis.set(Vector3.X);
        }

        // Get current position
        Vector3 position = new Vector3();
        animatingModel.transform.getTranslation(position);

        // Rotate around the model's center
        animatingModel.transform.translate(-position.x, -position.y, -position.z); // Move to origin
        animatingModel.transform.rotate(rotationAxis, REMOVE_ANIMATION_ROTATION_SPEED * delta); // Rotate
        animatingModel.transform.translate(position.x, position.y, position.z); // Move back

        // Check if animation is complete
        Vector3 currentPosition = animatingModel.transform.getTranslation(new Vector3());
        float distanceTraveled = currentPosition.dst(startPosition);

        if (animationTime >= REMOVE_ANIMATION_DURATION || distanceTraveled >= REMOVE_ANIMATION_DISTANCE) {
            isAnimating = false;
            animatingModel = null;
            direction.set(0, 0, 0);
            startPosition.set(0, 0, 0);
            animationTime = 0f;

            // Execute the callback to actually remove the model
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }

    public boolean isAnimating() {
        return isAnimating;
    }
}
