package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.ideaspace.IdeaSpace;
import com.ideaspace.models.ModelMesh;
import com.ideaspace.ui.components.ModelCard;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.HashMap;

public class ModelHandler {

    private IdeaSpace ideaSpace;

    public HashMap<String, ModelMesh> loadedModels;
    public HashMap<String, ModelMesh> modelLibrary;

    public ModelHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        loadedModels = new HashMap<>();
        modelLibrary = new HashMap<>();

    }

    public void loadInitialModels() {
        createModel("Background", "models/backgrounds/dark_background.glb");
        loadModel(modelLibrary.get("Background"));

        getModelInstance("Background").transform.idt().scale(20f, 20f, 20f);
    }

    public void createModels() {
        createModel("Arduino Uno", "models/microcontrollers/arduinouno.glb");
        createModel("Esp32", "models/microcontrollers/esp32.glb");
        createModel("Raspberry Pi", "models/microcontrollers/raspberry_pi.glb");
    }

    private void createModel(String name, String path) {
        ModelMesh modelMesh = new ModelMesh(name, path);
        modelLibrary.put(name, modelMesh);

        if (name.equals("Background")) return;

        ModelCard modelCard = new ModelCard(name);
        ideaSpace.controlPanel.addModelCardToLibrary(modelCard);
    }

    public void loadModel(ModelMesh modelMesh) {
        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal(modelMesh.modelPath));
        Scene scene = new Scene(sceneAsset.scene);

        modelMesh.setScene(scene);
        modelMesh.setModelSceneAsset(sceneAsset);

        loadedModels.put(modelMesh.modelName, modelMesh);
        ideaSpace.space.getSceneManager().addScene(modelMesh.getScene());
    }


    public ModelInstance getModelInstance(String name) {
        return loadedModels.get(name).getScene().modelInstance;
    }

    public void dispose() {
        for (ModelMesh modelMesh : loadedModels.values()) {
            modelMesh.getModelSceneAsset().dispose();
        }
    }

//    public void playAnimation(String name, String animationName) {
//        Scene scene = objects.get(name);
//        scene.animationController.animate(animationName, -1);
//    }


}
