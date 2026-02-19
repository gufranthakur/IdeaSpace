package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;

import com.badlogic.gdx.utils.GdxRuntimeException;
import com.ideaspace.IdeaSpace;
import com.ideaspace.models.ModelMesh;
import com.ideaspace.ui.components.ModelCard;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.HashMap;

public class ModelHandler {

    private IdeaSpace ideaSpace;
    private ModelMesh selectedModel;

    public HashMap<String, ModelMesh> loadedModels;
    public HashMap<String, ModelMesh> modelLibrary;

    public ModelHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        loadedModels = new HashMap<>();
        modelLibrary = new HashMap<>();
    }

    public void loadInitialModels() {
        addModelToLibrary("Background", "models/backgrounds/dark_background.glb");
        //loadModel(modelLibrary.get("Background"));
        //getModelInstance("Background").transform.idt().scale(10, 10, 10);


    }

    public void createModels() {
        addModelToLibrary("Arduino-Uno", "models/microcontrollers/arduino_uno.glb");
        addModelToLibrary("Esp32", "models/microcontrollers/esp32.glb");
        addModelToLibrary("Iphone-17", "models/misc/iphone17pro.glb");
        addModelToLibrary("Joystick-Module", "models/components/joystick_module.glb");
        addModelToLibrary("Servo-Motor", "models/components/servo_motor.glb");
        addModelToLibrary("l298motordriver", "models/components/l298motordriver.glb");
        addModelToLibrary("RaspberryPi", "models/microcontrollers/raspberry_pi.glb");
        addModelToLibrary("Rpi-cam", "models/components/rpi_cam.glb");
        addModelToLibrary("Mechanical-Keyboard", "models/misc/mechanical_keyboard.glb");
        addModelToLibrary("Mechanical-Keyboard", "models/misc/mechanical_keyboard.glb");





        loadModel(modelLibrary.get("Esp32"));
        getModelInstance("Esp32").transform.idt().scale(0.5f, 0.5f, 0.5f);

    }

    private void addModelToLibrary(String name, String path) {
        ModelMesh modelMesh = new ModelMesh(name, path);
        modelLibrary.put(name, modelMesh);

        if (name.equals("Background")) return;

        ModelCard modelCard = new ModelCard(this, modelMesh, false);
        ideaSpace.modelControlPanel.addModelCardToLibrary(modelCard);
    }

    public void loadModel(ModelMesh modelMesh) {
        if (loadedModels.containsKey(modelMesh.modelName)) {
            System.out.println("Model already exists!");
            return;
        }

        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal(modelMesh.modelPath));
        Scene scene = new Scene(sceneAsset.scene);

        modelMesh.setScene(scene);
        modelMesh.setModelSceneAsset(sceneAsset);

        loadedModels.put(modelMesh.modelName, modelMesh);
        ideaSpace.space.getSceneManager().addScene(modelMesh.getScene());

        // Update grab handler with current loaded models
        ideaSpace.space.getRightGrabHandler().setLoadedModels(loadedModels.values());
        ideaSpace.space.getLeftGrabHandler().setLoadedModels(loadedModels.values());

        if (modelMesh.modelName.equals("Background")) return;

        ModelCard modelCard = new ModelCard(this, modelMesh, true);
        ideaSpace.modelControlPanel.addModelCardToModelsPane(modelCard);

        selectedModel = modelMesh;
    }

    public void unloadModel(String modelName, ModelCard modelCard) {
        ModelMesh modelMesh;
        String nameToRemove;

        if (modelName == null) {
            modelMesh = selectedModel;
            nameToRemove = (selectedModel != null) ? selectedModel.modelName : null;
        } else {
            modelMesh = loadedModels.get(modelName);
            nameToRemove = modelName;
        }

        if (modelMesh != null) {
            ModelInstance modelInstance = modelMesh.getScene().modelInstance;

            final String finalNameToRemove = nameToRemove;
            final ModelMesh finalModelMesh = modelMesh;

            ideaSpace.animationHandler.removeModelAnimation(modelInstance, () -> {

                ideaSpace.space.getSceneManager().removeScene(finalModelMesh.getScene());
                finalModelMesh.dispose();
                loadedModels.remove(finalNameToRemove);

                // Update grab handler after removal
                ideaSpace.space.getRightGrabHandler().setLoadedModels(loadedModels.values());

                if (modelCard != null) {
                    ideaSpace.modelControlPanel.removeModelCard(modelCard);
                } else {
                    ideaSpace.modelControlPanel.removeModelCardByName(finalNameToRemove);
                }

                if (finalModelMesh == selectedModel) {
                    selectedModel = null;
                }
            });
        } else if (modelCard != null) {
            ideaSpace.modelControlPanel.removeModelCard(modelCard);
        }
    }

    public ModelInstance getModelInstance(String name) {
        return loadedModels.get(name).getScene().modelInstance;
    }

    public void dispose() {
        for (ModelMesh modelMesh : loadedModels.values()) {
            modelMesh.dispose();
        }
    }

    public void loadRandomModel() {
        if (modelLibrary.isEmpty()) {
            System.out.println("Model library is empty!");
            return;
        }

        java.util.List<String> availableModels = new java.util.ArrayList<>();
        for (String modelName : modelLibrary.keySet()) {
            if (!modelName.equals("Background")) {
                availableModels.add(modelName);
            }
        }

        if (availableModels.isEmpty()) {
            System.out.println("No models available to load!");
            return;
        }

        int randomIndex = (int) (Math.random() * availableModels.size());
        String randomModelName = availableModels.get(randomIndex);

        ModelMesh randomModel = modelLibrary.get(randomModelName);
        loadModel(randomModel);
        selectedModel = randomModel;
    }

    public void splitModel() {
        if (selectedModel == null) {
            System.out.println("No model selected!");
            return;
        }

        Scene scene = selectedModel.getScene();

        try {
            if (scene.animationController != null) {
                scene.animationController.animate("split", 1, 1f, null, 0f);
            }
        } catch (GdxRuntimeException e) {
            try {
                if (scene.animationController != null) {
                    scene.animationController.animate("Split", 1, 1f, null, 0f);
                }
            } catch (GdxRuntimeException exception) {
                System.out.println("Model has no split animation");
            }
        }
    }

    public ModelMesh getSelectedModel() {
        return selectedModel;
    }

    public IdeaSpace getIdeaSpace() {
        return ideaSpace;
    }
}
