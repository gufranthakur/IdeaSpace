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

    private static final int THUMB_TIP = 4;
    private static final int INDEX_TIP = 8;

    private SimulationHand hand;
    private Camera camera;
    private Collection<ModelMesh> loadedModels;

    private boolean isGrabbing = false;
    private ModelMesh grabbedModel = null;

    // --- Left hand only ---
    public float maxGrabDistance = 20.0f;
    private float grabDistance;
    private Vector3 grabOffset = new Vector3();
    private float positionLerpFactor = 0.15f;
    public float positionMultiplier = 3.0f;
    private Vector3 targetPosition = new Vector3();

    // --- Right hand only ---
    public float rotationMultiplier = 70f;
    private Vector3 lastPinchCenter = new Vector3();
    private boolean hasLastPinchCenter = false;

    // Snapshot of object rotation at grab start
    private Quaternion rotationAtGrabStart = new Quaternion();
    // Accumulated delta since this grab session started
    private float sessionYaw   = 0f;
    private float sessionPitch = 0f;

    // Temp
    private Vector3 thumbPos = new Vector3();
    private Vector3 indexPos = new Vector3();
    private Vector3 pinchCenter = new Vector3();
    private Vector3 rayDir = new Vector3();
    private Vector3 tempVec = new Vector3();
    private Vector3 intersection = new Vector3();
    private Matrix4 tempTransform = new Matrix4();
    private Quaternion yawQuat   = new Quaternion();
    private Quaternion pitchQuat = new Quaternion();
    private Quaternion tempQuat  = new Quaternion();

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
        if (hand == null || loadedModels == null || !hand.hasData) return;

        thumbPos.set(hand.getPosition(THUMB_TIP));
        indexPos.set(hand.getPosition(INDEX_TIP));

        boolean isIndexPinching = thumbPos.dst(indexPos) < PINCH_THRESHOLD;
        pinchCenter.set(thumbPos).add(indexPos).scl(0.5f);

        if (isIndexPinching && !isGrabbing) {
            tryGrabWithRaycast();
        } else if (!isIndexPinching && isGrabbing) {
            release();
        } else if (isGrabbing && grabbedModel != null && isIndexPinching) {
            if (isRightHand) updateRotation();
            else             updateGrabbedObjectPosition();
        }

        if (isIndexPinching) {
            lastPinchCenter.set(pinchCenter);
            hasLastPinchCenter = true;
        } else {
            hasLastPinchCenter = false;
        }
    }

    private void tryGrabWithRaycast() {
        rayDir.set(pinchCenter).sub(camera.position).nor();
        ray.set(camera.position, rayDir);

        ModelMesh closestModel = null;
        float closestDistance  = Float.MAX_VALUE;

        for (ModelMesh model : loadedModels) {
            if (model.modelName.equals("Spaceship")
                || model.modelName.equals("Vintage")
                || model.modelName.equals("Office")
                || model.modelName.equals("Classroom")
                || model.modelName.equals("Cube")) continue;
            if (model.getScene() == null || model.getScene().modelInstance == null) continue;

            model.getScene().modelInstance.calculateBoundingBox(boundingBox);
            boundingBox.mul(model.getScene().modelInstance.transform);

            if (com.badlogic.gdx.math.Intersector.intersectRayBounds(ray, boundingBox, intersection)) {
                float distance = camera.position.dst(intersection);
                if (distance < closestDistance && distance < RAY_LENGTH && distance <= maxGrabDistance) {
                    closestDistance = distance;
                    closestModel    = model;
                }
            }
        }

        if (closestModel != null) {
            grabbedModel = closestModel;
            isGrabbing   = true;

            if (!isRightHand) {
                grabDistance = closestDistance;
                tempVec.set(rayDir).scl(grabDistance).add(camera.position);
                Vector3 objectPos = grabbedModel.getScene().modelInstance.transform.getTranslation(new Vector3());
                grabOffset.set(objectPos).sub(tempVec);
                targetPosition.set(objectPos);
            } else {
                // Snapshot the current rotation and reset session deltas
                grabbedModel.getScene().modelInstance.transform.getRotation(rotationAtGrabStart);
                sessionYaw   = 0f;
                sessionPitch = 0f;
            }

            hasLastPinchCenter = false;
        }
    }

    private void updateRotation() {
        if (grabbedModel == null || !hasLastPinchCenter) return;

        float deltaX = pinchCenter.x - lastPinchCenter.x;
        float deltaY = pinchCenter.y - lastPinchCenter.y;

        if (Math.abs(deltaX) >= Math.abs(deltaY)) deltaY = 0f;
        else                                       deltaX = 0f;

        sessionYaw   +=  deltaX * rotationMultiplier;
        sessionPitch += -deltaY * rotationMultiplier;

        // Build delta rotation for this session in world space
        yawQuat.set(Vector3.Y, sessionYaw);
        pitchQuat.set(Vector3.X, sessionPitch);
        tempQuat.set(yawQuat).mul(pitchQuat);

        // Apply session delta ON TOP of the snapshotted rotation
        tempQuat.mul(rotationAtGrabStart);

        Vector3 currentPos = grabbedModel.getScene().modelInstance.transform.getTranslation(tempVec);
        tempTransform.set(currentPos, tempQuat);
        grabbedModel.getScene().modelInstance.transform.set(tempTransform);
    }

    private void updateGrabbedObjectPosition() {
        if (grabbedModel == null || grabbedModel.getScene() == null) return;

        rayDir.set(pinchCenter).sub(camera.position).nor();
        targetPosition.set(rayDir).scl(grabDistance).add(camera.position).add(grabOffset);

        Vector3 currentPos     = grabbedModel.getScene().modelInstance.transform.getTranslation(tempVec);
        Vector3 movementDelta  = targetPosition.cpy().sub(currentPos).scl(positionMultiplier);
        Vector3 amplifiedTarget = currentPos.cpy().add(movementDelta);
        currentPos.lerp(amplifiedTarget, positionLerpFactor);

        tempTransform.set(grabbedModel.getScene().modelInstance.transform);
        tempTransform.setTranslation(currentPos);
        grabbedModel.getScene().modelInstance.transform.set(tempTransform);
    }

    private void release() {
        grabbedModel       = null;
        isGrabbing         = false;
        hasLastPinchCenter = false;
        grabOffset.setZero();
        lastPinchCenter.setZero();
        sessionYaw   = 0f;
        sessionPitch = 0f;
    }

    public boolean isGrabbing()          { return isGrabbing; }
    public ModelMesh getGrabbedModel()   { return grabbedModel; }
    public void dispose()                { if (isGrabbing) release(); }
}
