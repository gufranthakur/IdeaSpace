package com.ideaspace.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.ideaspace.core.Space;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.HashMap;

public class Panel {

    private Space space;

    private HashMap<String, Scene> objects;
    private HashMap<String, SceneAsset> objectAssets;

    public Panel(Space space) {
        this.space = space;

        objects = new HashMap<>();
        objectAssets = new HashMap<>();
    }

    public void loadObject(String name, String path) {
        SceneAsset asset = new GLBLoader().load(Gdx.files.internal(path));
        Scene scene = new Scene(asset.scene);

        objects.put(name, scene);
        objectAssets.put(name, asset);

        space.getSceneManager().addScene(scene);
    }

    public void translateObject(ModelInstance instance, float x, float y, float z) {
        instance.transform.idt().translate(x, y, z);
    }


    //----------------Getters and Setters------------------//

    public HashMap<String, Scene> getObjects() {
        return objects;
    }

    public HashMap<String, SceneAsset> getObjectAssets() {
        return objectAssets;
    }

    public ModelInstance getModelInstanceOf(String name) {
        return getObjects().get(name).modelInstance;
    }


}
