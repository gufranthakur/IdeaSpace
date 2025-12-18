package com.ideaspace.models;

import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

public class ModelMesh {

    public String modelPath, modelName;
    private Scene modelScene;
    private SceneAsset modelSceneAsset;

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

}
