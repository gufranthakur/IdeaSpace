package com.ideaspace.handlers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.ideaspace.models.ModelMesh;
import com.ideaspace.simulationhand.SimulationHand;

import java.util.Collection;

public class GrabHandler {

    private static final float PINCH_THRESHOLD = 0.15f;
    private static final float RAY_LENGTH = 100f;

    private static final int WRIST = 0;
    private static final int THUMB_TIP = 4;
    private static final int INDEX_TIP = 8;

    private SimulationHand hand;
    private Camera camera;
    private Collection<ModelMesh> loadedModels;

    private boolean isGrabbing = false;
    private boolean isRotating = false;
    private ModelMesh grabbedModel = null;

    public float maxGrabDistance = 20.0f;
    private float grabDistance;
    private Vector3 grabOffset = new Vector3();

    // Lerp factors
    private float positionLerpFactor = 0.15f;
    private float rotationLerpFactor = 0.25f;

    // Movement multipliers (applied before lerping)
    public float positionMultiplier = 3.0f;
    public float rotationMultiplier = 7.5f;

    // Target values for lerping
    private Vector3 targetPosition = new Vector3();
    private Quaternion targetRotation = new Quaternion();

    // Rotation tracking
    private Vector3 initialRotationVector = new Vector3();
    private Quaternion initialRotation = new Quaternion();
    private Quaternion currentRotation = new Quaternion();
    private Quaternion deltaRotation = new Quaternion();

    // Temp vectors
    private Vector3 wristPos = new Vector3();
    private Vector3 thumbPos = new Vector3();
    private Vector3 indexPos = new Vector3();
    private Vector3 pinchCenter = new Vector3();
    private Vector3 rayDir = new Vector3();
    private Vector3 tempVec = new Vector3();
    private Vector3 intersection = new Vector3();
    private Vector3 rotationVector = new Vector3();
    private Matrix4 tempTransform = new Matrix4();

    private Ray ray = new Ray();
    private BoundingBox boundingBox = new BoundingBox();


    private boolean isRightHand;
    public GrabHandler(SimulationHand hand, Camera camera, boolean isRightHand) {
        this.hand = hand;
        this.camera = camera;
        this.isRightHand = isRightHand;
    }

    public void setLoadedModels(Collection<ModelMesh> loadedModels) {
        this.loadedModels = loadedModels;
    }

    public void update() {
        if (hand == null || loadedModels == null) return;

        if (!hand.hasData) return;

        wristPos.set(hand.getPosition(WRIST));
        thumbPos.set(hand.getPosition(THUMB_TIP));
        indexPos.set(hand.getPosition(INDEX_TIP));

        float indexPinchDistance = thumbPos.dst(indexPos);
        boolean isIndexPinching = indexPinchDistance < PINCH_THRESHOLD;

        // Grab with thumb-index
        if (isIndexPinching && !isGrabbing) {
            pinchCenter.set(thumbPos).add(indexPos).scl(0.5f);
            tryGrabWithRaycast();
        }
        // Release grab
        else if (!isIndexPinching && isGrabbing) {
            release();
        }
        // Update grabbed object (includes rotation)
        else if (isGrabbing && grabbedModel != null && isIndexPinching) {
            pinchCenter.set(thumbPos).add(indexPos).scl(0.5f);

            if (!isRotating) {
                startRotation();
            }

            updateRotation();
            updateGrabbedObject();
        }
    }

    private void tryGrabWithRaycast() {
        rayDir.set(pinchCenter).sub(camera.position).nor();
        ray.set(camera.position, rayDir);

        ModelMesh closestModel = null;
        float closestDistance = Float.MAX_VALUE;
        Vector3 closestIntersection = new Vector3();

        for (ModelMesh model : loadedModels) {
            if (model.modelName.equals("Background") || model.modelName.equals("Room")) continue;
            if (model.getScene() == null || model.getScene().modelInstance == null) continue;

            model.getScene().modelInstance.calculateBoundingBox(boundingBox);
            boundingBox.mul(model.getScene().modelInstance.transform);

            if (com.badlogic.gdx.math.Intersector.intersectRayBounds(ray, boundingBox, intersection)) {
                float distance = camera.position.dst(intersection);
                if (distance < closestDistance && distance < RAY_LENGTH && distance <= maxGrabDistance) {
                    closestDistance = distance;
                    closestModel = model;
                    closestIntersection.set(intersection);
                }
            }
        }

        if (closestModel != null) {
            grabbedModel = closestModel;
            grabDistance = closestDistance;

            tempVec.set(rayDir).scl(grabDistance).add(camera.position);
            Vector3 objectPos = grabbedModel.getScene().modelInstance.transform.getTranslation(new Vector3());
            grabOffset.set(objectPos).sub(tempVec);

            // Initialize target position to current position
            targetPosition.set(objectPos);

            isGrabbing = true;
        }
    }

    private void startRotation() {
        if (grabbedModel == null) return;

        // Store initial rotation vector (wrist to pinch center)
        initialRotationVector.set(pinchCenter).sub(wristPos).nor();

        // Store initial object rotation
        grabbedModel.getScene().modelInstance.transform.getRotation(initialRotation);
        targetRotation.set(initialRotation);

        isRotating = true;
    }

    private void updateRotation() {
        if (grabbedModel == null) return;

        // Current rotation vector (wrist to pinch center)
        rotationVector.set(pinchCenter).sub(wristPos).nor();

        // Calculate rotation between initial and current vectors
        deltaRotation.setFromCross(initialRotationVector, rotationVector);

        // Apply rotation multiplier by scaling the angle
        float angle = deltaRotation.getAxisAngle(tempVec);
        deltaRotation.setFromAxis(tempVec, angle * rotationMultiplier);

        // Calculate target rotation
        targetRotation.set(initialRotation).mul(deltaRotation);

        // Get current rotation and slerp towards target
        Quaternion currentRot = grabbedModel.getScene().modelInstance.transform.getRotation(new Quaternion());
        currentRot.slerp(targetRotation, rotationLerpFactor);

        // Apply slerped rotation while preserving position
        Vector3 currentPos = grabbedModel.getScene().modelInstance.transform.getTranslation(tempVec);
        tempTransform.set(grabbedModel.getScene().modelInstance.transform);
        tempTransform.set(currentPos, currentRot);
        grabbedModel.getScene().modelInstance.transform.set(tempTransform);

        // Update initial for continuous rotation
        initialRotationVector.set(rotationVector);
        grabbedModel.getScene().modelInstance.transform.getRotation(initialRotation);
    }

    private void stopRotation() {
        isRotating = false;
        initialRotationVector.setZero();
    }

    private void release() {
        grabbedModel = null;
        isGrabbing = false;
        isRotating = false;
        grabOffset.setZero();
        initialRotationVector.setZero();
    }

    private void updateGrabbedObject() {
        if (grabbedModel == null || grabbedModel.getScene() == null) return;

        rayDir.set(pinchCenter).sub(camera.position).nor();
        targetPosition.set(rayDir).scl(grabDistance).add(camera.position).add(grabOffset);

        // Apply position multiplier to the movement delta
        Vector3 currentPos = grabbedModel.getScene().modelInstance.transform.getTranslation(tempVec);
        Vector3 movementDelta = targetPosition.cpy().sub(currentPos).scl(positionMultiplier);
        Vector3 amplifiedTarget = currentPos.cpy().add(movementDelta);

        // Lerp current position towards amplified target
        currentPos.lerp(amplifiedTarget, positionLerpFactor);

        tempTransform.set(grabbedModel.getScene().modelInstance.transform);
        tempTransform.setTranslation(currentPos);
        grabbedModel.getScene().modelInstance.transform.set(tempTransform);
    }

    public boolean isGrabbing() {
        return isGrabbing;
    }

    public ModelMesh getGrabbedModel() {
        return grabbedModel;
    }

    public void dispose() {
        if (isGrabbing) {
            release();
        }
    }
}
