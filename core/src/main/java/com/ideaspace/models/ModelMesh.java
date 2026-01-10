package com.ideaspace.models;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
import com.badlogic.gdx.physics.bullet.dynamics.btRigidBody;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;



public class ModelMesh {

    public String modelPath, modelName;
    private Scene modelScene;
    private SceneAsset modelSceneAsset;
    private boolean isLoaded = false;

    // Bullet physics components
    private btRigidBody rigidBody;
    private btCollisionShape collisionShape;
    private btMotionState motionState;
    private float mass = 1f;

    public ModelMesh(String modelName, String modelPath) {
        this.modelName = modelName;
        this.modelPath = modelPath;
        this.modelSceneAsset = null;
        this.modelScene = null;
    }

    public void setScene(Scene scene) {
        this.modelScene = scene;
    }

    public void setModelSceneAsset(SceneAsset sceneAsset) {
        this.modelSceneAsset = sceneAsset;
    }

    public Scene getScene() {
        return modelScene;
    }

    public SceneAsset getModelSceneAsset() {
        return modelSceneAsset;
    }

    public void setIsLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }

    public boolean isLoaded() {
        return isLoaded;
    }

    // Create physics body
    public void createPhysicsBody(btCollisionShape shape, float mass, Vector3 position) {
        this.collisionShape = shape;
        this.mass = mass;

        Matrix4 transform = new Matrix4();
        transform.setToTranslation(position);

        if (modelScene != null) {
            modelScene.modelInstance.transform.set(transform);
        }

        motionState = new btMotionState() {
            @Override
            public void getWorldTransform(Matrix4 worldTrans) {
                worldTrans.set(modelScene.modelInstance.transform);
            }

            @Override
            public void setWorldTransform(Matrix4 worldTrans) {
                modelScene.modelInstance.transform.set(worldTrans);
            }
        };

        Vector3 localInertia = new Vector3();
        if (mass > 0f) {
            collisionShape.calculateLocalInertia(mass, localInertia);
        }

        btRigidBody.btRigidBodyConstructionInfo info =
            new btRigidBody.btRigidBodyConstructionInfo(mass, motionState, collisionShape, localInertia);
        rigidBody = new btRigidBody(info);

        // Reduce bounciness and add friction
        rigidBody.setRestitution(0.1f);  // Very low bounce (0-1, default 0)
        rigidBody.setFriction(0.8f);     // High friction (0-1, default 0.5)
        rigidBody.setDamping(0.3f, 0.5f); // Linear and angular damping

        info.dispose();
    }

    public btRigidBody getRigidBody() {
        return rigidBody;
    }

    public void setMass(float mass) {
        this.mass = mass;
    }

    public void dispose() {
        if (rigidBody != null) rigidBody.dispose();
        if (collisionShape != null) collisionShape.dispose();
        if (motionState != null) motionState.dispose();
        if (modelSceneAsset != null) modelSceneAsset.dispose();
    }
}
