package com.ideaspace.handlers;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.math.Intersector;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.math.collision.Ray;
import com.ideaspace.models.ModelMesh;
import com.ideaspace.models.SplitPiece;
import com.ideaspace.simulationhand.SimulationHand;

import java.util.Collection;

public class GrabHandler {

    private static final float PINCH_THRESHOLD = 0.25f;
    private static final float FIST_THRESHOLD  = 0.6f;
    private static final float RAY_LENGTH      = 100f;

    private static final int WRIST      = 0;
    private static final int THUMB_TIP  = 4;
    private static final int INDEX_TIP  = 8;
    private static final int MIDDLE_TIP = 12;
    private static final int RING_TIP   = 16;
    private static final int PINKY_TIP  = 20;

    private SimulationHand hand;
    private Camera camera;
    private Collection<ModelMesh> loadedModels;
    private Collection<SplitPiece> splitPieces = null;
    private ModelHandler modelHandler;

    private boolean isGrabbing = false;
    private ModelMesh grabbedModel = null;
    private SplitPiece grabbedPiece = null;
    private boolean wasFist = false;

    public float maxGrabDistance = 20.0f;
    private float grabDistance;
    private Vector3 grabOffset = new Vector3();
    public float positionMultiplier = 3.0f;
    private Vector3 targetPosition = new Vector3();

    public float rotationMultiplier = 120f;
    private Vector3 lastPinchCenter = new Vector3();
    private boolean hasLastPinchCenter = false;

    private Quaternion rotationAtGrabStart = new Quaternion();
    private float sessionYaw   = 0f;
    private float sessionPitch = 0f;

    public float grabSphereRadius = 0.5f;

    // Temp
    private Vector3 thumbPos      = new Vector3();
    private Vector3 indexPos      = new Vector3();
    private Vector3 pinchCenter   = new Vector3();
    private Vector3 rayDir        = new Vector3();
    private Vector3 tempVec       = new Vector3();
    private Matrix4 tempTransform = new Matrix4();
    private Quaternion yawQuat    = new Quaternion();
    private Quaternion pitchQuat  = new Quaternion();
    private Quaternion tempQuat   = new Quaternion();

    private Ray ray = new Ray();
    private boolean isRightHand;

    public GrabHandler(SimulationHand hand, Camera camera, boolean isRightHand, ModelHandler modelHandler) {
        this.hand = hand;
        this.camera = camera;
        this.isRightHand = isRightHand;
        this.modelHandler = modelHandler;
    }

    public void setLoadedModels(Collection<ModelMesh> loadedModels) {
        this.loadedModels = loadedModels;
    }

    public void setSplitPieces(Collection<SplitPiece> splitPieces) {
        this.splitPieces = splitPieces;
        System.out.println("[GRAB] splitPieces updated: "
            + (splitPieces == null ? "null" : splitPieces.size() + " pieces"));
    }

    public void update() {
        if (hand == null || loadedModels == null || !hand.hasData) return;

        thumbPos.set(hand.getPosition(THUMB_TIP));
        indexPos.set(hand.getPosition(INDEX_TIP));

        // Fist detection — right hand only, runs first
        if (isRightHand) {
            Vector3 wristPos  = hand.getPosition(WRIST);
            Vector3 indexTip  = hand.getPosition(INDEX_TIP);
            Vector3 middleTip = hand.getPosition(MIDDLE_TIP);
            Vector3 ringTip   = hand.getPosition(RING_TIP);
            Vector3 pinkyTip  = hand.getPosition(PINKY_TIP);

            boolean isFist = wristPos.dst(indexTip)  < FIST_THRESHOLD
                && wristPos.dst(middleTip) < FIST_THRESHOLD
                && wristPos.dst(ringTip)   < FIST_THRESHOLD
                && wristPos.dst(pinkyTip)  < FIST_THRESHOLD;

            if (isFist && !wasFist) {
                System.out.println("[FIST] Fist detected on right hand.");
                // Release any active grab when fist starts
                if (isGrabbing) release();
            }
            wasFist = isFist;

            // Block pinch logic entirely while fist is active
            if (wasFist) return;
        }

        boolean isIndexPinching = thumbPos.dst(indexPos) < PINCH_THRESHOLD;
        pinchCenter.set(thumbPos).add(indexPos).scl(0.5f);

        if (isIndexPinching && !isGrabbing) {
            tryGrabWithRaycast();
        } else if (!isIndexPinching && isGrabbing) {
            release();
        } else if (isGrabbing && isIndexPinching) {
            if (isRightHand) updateGrabbedObjectPosition();
            else             updateRotation();
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

        ModelMesh closestModel  = null;
        SplitPiece closestPiece = null;
        float closestDistance   = Float.MAX_VALUE;

        // --- Check whole models ---
        for (ModelMesh model : loadedModels) {
            if (isBackground(model.modelName)) continue;
            if (model.getScene() == null || model.getScene().modelInstance == null) continue;

            Vector3 modelPos = model.getScene().modelInstance.transform.getTranslation(new Vector3());
            float distance = camera.position.dst(modelPos);

            if (distance > RAY_LENGTH || distance > maxGrabDistance) continue;

            if (Intersector.intersectRaySphere(ray, modelPos, grabSphereRadius, null)) {
                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestModel    = model;
                }
            }
        }

        // --- Check split pieces ---
        if (splitPieces != null) {
            for (SplitPiece piece : splitPieces) {
                if (piece.getModelInstance() == null) continue;

                BoundingBox bbox = new BoundingBox();
                piece.getModelInstance().calculateBoundingBox(bbox);
                bbox.mul(piece.getModelInstance().transform);

                Vector3 piecePos = new Vector3();
                bbox.getCenter(piecePos);

                float distance = camera.position.dst(piecePos);
                if (distance > maxGrabDistance) continue;

                if (Intersector.intersectRaySphere(ray, piecePos, grabSphereRadius, null)) {
                    if (distance < closestDistance) {
                        closestDistance = distance;
                        closestPiece    = piece;
                        closestModel    = null;
                    }
                }
            }
        }

        // --- Grab a split piece ---
        if (closestPiece != null) {
            grabbedPiece = closestPiece;
            grabbedModel = null;
            isGrabbing   = true;
            hasLastPinchCenter = false;
            System.out.println("[GRAB] Grabbed piece: " + grabbedPiece.nodeName);

            if (!isRightHand) {
                grabbedPiece.getModelInstance().transform.getRotation(rotationAtGrabStart);
                sessionYaw   = 0f;
                sessionPitch = 0f;
            }

            // --- Grab a whole model ---
        } else if (closestModel != null) {
            grabbedModel = closestModel;
            grabbedPiece = null;
            isGrabbing   = true;
            hasLastPinchCenter = false;
            System.out.println("[GRAB] Grabbed model: " + grabbedModel.modelName);

            if (modelHandler != null) modelHandler.setSelectedModel(grabbedModel);

            if (isRightHand) {
                grabDistance = closestDistance;
                tempVec.set(rayDir).scl(grabDistance).add(camera.position);
                Vector3 objectPos = grabbedModel.getScene().modelInstance.transform.getTranslation(new Vector3());
                grabOffset.set(objectPos).sub(tempVec);
                targetPosition.set(objectPos);
            } else {
                grabbedModel.getScene().modelInstance.transform.getRotation(rotationAtGrabStart);
                sessionYaw   = 0f;
                sessionPitch = 0f;
            }
        }
    }

    private void updateGrabbedObjectPosition() {
        if (!hasLastPinchCenter) return;

        // --- Move a split piece ---
        if (grabbedPiece != null) {
            Vector3 delta = tempVec.set(pinchCenter).sub(lastPinchCenter).scl(positionMultiplier);
            Vector3 currentPos = grabbedPiece.getModelInstance().transform.getTranslation(new Vector3());
            currentPos.add(delta);
            tempTransform.set(grabbedPiece.getModelInstance().transform);
            tempTransform.setTranslation(currentPos);
            grabbedPiece.getModelInstance().transform.set(tempTransform);
            return;
        }

        // --- Move a whole model ---
        if (grabbedModel == null || grabbedModel.getScene() == null) return;
        if (isBackground(grabbedModel.modelName)) return;

        Vector3 delta = tempVec.set(pinchCenter).sub(lastPinchCenter).scl(positionMultiplier);
        Vector3 currentPos = grabbedModel.getScene().modelInstance.transform.getTranslation(new Vector3());
        currentPos.add(delta);
        tempTransform.set(grabbedModel.getScene().modelInstance.transform);
        tempTransform.setTranslation(currentPos);
        grabbedModel.getScene().modelInstance.transform.set(tempTransform);
    }

    private void updateRotation() {
        if (!hasLastPinchCenter) return;

        com.badlogic.gdx.graphics.g3d.ModelInstance target = grabbedPiece != null
            ? grabbedPiece.getModelInstance()
            : (grabbedModel != null ? grabbedModel.getScene().modelInstance : null);
        if (target == null) return;

        float deltaX = pinchCenter.x - lastPinchCenter.x;
        float deltaY = pinchCenter.y - lastPinchCenter.y;

        if (Math.abs(deltaX) >= Math.abs(deltaY)) deltaY = 0f;
        else                                       deltaX = 0f;

        sessionYaw   +=  deltaX * rotationMultiplier;
        sessionPitch += -deltaY * rotationMultiplier;

        yawQuat.set(Vector3.Y, sessionYaw);
        pitchQuat.set(Vector3.X, sessionPitch);
        tempQuat.set(yawQuat).mul(pitchQuat).mul(rotationAtGrabStart);

        Vector3 currentPos = target.transform.getTranslation(tempVec);
        tempTransform.set(currentPos, tempQuat);
        target.transform.set(tempTransform);
    }

    private void release() {
        System.out.println("[GRAB] Released: "
            + (grabbedPiece != null ? "piece " + grabbedPiece.nodeName
            : grabbedModel  != null ? "model " + grabbedModel.modelName
            : "nothing"));
        grabbedModel       = null;
        grabbedPiece       = null;
        isGrabbing         = false;
        hasLastPinchCenter = false;
        grabOffset.setZero();
        lastPinchCenter.setZero();
        sessionYaw   = 0f;
        sessionPitch = 0f;
    }

    private boolean isBackground(String name) {
        return name.equals("Spaceship") || name.equals("Light") || name.equals("Dark");
    }

    public boolean isFist()              { return wasFist; }
    public boolean isGrabbing()          { return isGrabbing; }
    public ModelMesh getGrabbedModel()   { return grabbedModel; }
    public void dispose()                { if (isGrabbing) release(); }
}
