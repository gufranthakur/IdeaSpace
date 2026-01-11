package com.ideaspace.handlers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Matrix4;
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

    // Configurable grab distance limit
    public float maxGrabDistance = 5.0f;

    // Distance from camera to grabbed object (maintained while dragging)
    private float grabDistance;

    // Offset from pinch center to object center when grabbed
    private Vector3 grabOffset = new Vector3();

    // Temp vectors
    private Vector3 thumbPos = new Vector3();
    private Vector3 indexPos = new Vector3();
    private Vector3 pinchCenter = new Vector3();
    private Vector3 rayDir = new Vector3();
    private Vector3 tempVec = new Vector3();
    private Vector3 intersection = new Vector3();
    private Matrix4 tempTransform = new Matrix4();

    // Ray for raycasting
    private Ray ray = new Ray();
    private BoundingBox boundingBox = new BoundingBox();

    public GrabHandler(SimulationHand hand, Camera camera) {
        this.hand = hand;
        this.camera = camera;
    }

    public void setLoadedModels(Collection<ModelMesh> loadedModels) {
        this.loadedModels = loadedModels;
    }

    public void update() {
        if (hand == null || loadedModels == null) return;

        thumbPos.set(hand.getPosition(THUMB_TIP));
        indexPos.set(hand.getPosition(INDEX_TIP));

        float pinchDistance = thumbPos.dst(indexPos);
        pinchCenter.set(thumbPos).add(indexPos).scl(0.5f);

        boolean isPinching = pinchDistance < PINCH_THRESHOLD;

        if (isPinching && !isGrabbing) {
            tryGrabWithRaycast();
        } else if (!isPinching && isGrabbing) {
            release();
        } else if (isGrabbing && grabbedModel != null) {
            updateGrabbedObject();
        }
    }

    private void tryGrabWithRaycast() {
        // Ray from camera through pinch center
        rayDir.set(pinchCenter).sub(camera.position).nor();
        ray.set(camera.position, rayDir);

        ModelMesh closestModel = null;
        float closestDistance = Float.MAX_VALUE;
        Vector3 closestIntersection = new Vector3();

        // Check each model's AABB
        for (ModelMesh model : loadedModels) {
            if (model.getScene() == null || model.getScene().modelInstance == null) continue;

            // Get model's bounding box
            model.getScene().modelInstance.calculateBoundingBox(boundingBox);
            boundingBox.mul(model.getScene().modelInstance.transform);

            // Test ray intersection with AABB
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

            // Calculate offset from pinch center to object center
            tempVec.set(rayDir).scl(grabDistance).add(camera.position);
            Vector3 objectPos = grabbedModel.getScene().modelInstance.transform.getTranslation(new Vector3());
            grabOffset.set(objectPos).sub(tempVec);

            isGrabbing = true;
        }
    }

    private void release() {
        grabbedModel = null;
        isGrabbing = false;
        grabOffset.setZero();
    }

    private void updateGrabbedObject() {
        if (grabbedModel == null || grabbedModel.getScene() == null) return;

        // Calculate ray direction from camera through current pinch center
        rayDir.set(pinchCenter).sub(camera.position).nor();

        // New position at the same distance as when grabbed, plus the grab offset
        tempVec.set(rayDir).scl(grabDistance).add(camera.position).add(grabOffset);

        // Update transform
        tempTransform.set(grabbedModel.getScene().modelInstance.transform);
        tempTransform.setTranslation(tempVec);
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
