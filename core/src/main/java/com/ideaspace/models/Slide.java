package com.ideaspace.models;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.ideaspace.core.Space;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.HashMap;
import java.util.Iterator;

public class Slide {

    private Space space;

    private HashMap<String, Scene> objects;
    private HashMap<String, SceneAsset> objectAssets;
    public Scene selectedObject;

    private Iterator<String> modelIterator;
    private String currentModel;

    public Slide(Space space) {
        this.space = space;

        objects = new HashMap<>();
        objectAssets = new HashMap<>();
    }

    public void loadObject(String name, String path) {
        SceneAsset asset = new GLBLoader().load(Gdx.files.internal(path));
        Scene scene = new Scene(asset.scene);

        objects.put(name, scene);
        objectAssets.put(name, asset);

        selectedObject = objects.get(name);
    }

    public void addObject(String name) {
        space.getSceneManager().addScene(objects.get(name));
    }

    public void playAnimation(String name, String animationName) {
        Scene scene = objects.get(name);
        scene.animationController.animate(animationName, -1);

    }

    public void swapModel(String removedModel, String newModel) {
        space.getSceneManager().removeScene(getObjects().get(removedModel));
        space.getSceneManager().addScene(getObjects().get(newModel));
    }

    public void nextModel() {
        if (modelIterator == null || !modelIterator.hasNext()) {
            modelIterator = getObjects().keySet().iterator();
        }

        if (currentModel != null) {
            space.getSceneManager().removeScene(getObjects().get(currentModel));
        }

        if (modelIterator.hasNext()) {
            currentModel = modelIterator.next();
            space.getSceneManager().addScene(getObjects().get(currentModel));
        }
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
