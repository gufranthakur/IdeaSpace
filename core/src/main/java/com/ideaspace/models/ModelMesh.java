package com.ideaspace.models;

import net.mgsx.gltf.scene3d.scene.Scene;

public class ModelMesh {

    public String modelPath, modelName;
    private Scene modelScene;

    public ModelMesh(String modelName, String modelPath) {
        this.modelName = modelName;
        this.modelPath = modelPath;
    }

    public void setScene(Scene scene) {
        this.modelScene = scene;
    }

    public Scene getScene() {
        return modelScene;
    }

}
