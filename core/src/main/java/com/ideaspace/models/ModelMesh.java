package com.ideaspace.models;

import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class ModelMesh {

    public String modelPath, modelName;
    private Scene modelScene;
    private SceneAsset modelSceneAsset;
    private boolean isLoaded = false;
    public boolean isSplit = false;

    // Bullet physics components

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

    public void dispose() {

    }

}
