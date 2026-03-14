package com.ideaspace.models;

import com.badlogic.gdx.graphics.g3d.ModelInstance;
import net.mgsx.gltf.scene3d.scene.Scene;

public class SplitPiece {

    public final String nodeName;
    public final String parentModelName;
    private final Scene scene;

    public SplitPiece(String nodeName, String parentModelName, Scene scene) {
        this.nodeName = nodeName;
        this.parentModelName = parentModelName;
        this.scene = scene;
    }

    public Scene getScene()                { return scene; }
    public ModelInstance getModelInstance() { return scene.modelInstance; }
}
