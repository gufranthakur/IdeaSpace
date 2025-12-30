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

    private ModelMesh selectedModel; // Add this as a class field at the top with other fields

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

        getModelInstance("Background").transform.idt().scale(50f, 50f, 50f);
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

        ModelCard modelCard = new ModelCard(this, modelMesh, false);
        ideaSpace.controlPanel.addModelCardToLibrary(modelCard);
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

        if (modelMesh.modelName.equals("Background")) return;

        ModelCard modelCard = new ModelCard(this, modelMesh, true);
        ideaSpace.controlPanel.addModelCardToModelsPane(modelCard);

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

            // Capture the name for removal in the lambda
            final String finalNameToRemove = nameToRemove;
            final ModelMesh finalModelMesh = modelMesh;

            // Play remove animation first
            ideaSpace.animationHandler.removeModelAnimation(modelInstance, () -> {
                // This code runs after animation completes
                ideaSpace.space.getSceneManager().removeScene(finalModelMesh.getScene());
                finalModelMesh.getModelSceneAsset().dispose();
                loadedModels.remove(finalNameToRemove);

                // Find and remove the corresponding ModelCard
                if (modelCard != null) {
                    ideaSpace.controlPanel.removeModelCard(modelCard);
                } else {
                    // Search for the card by matching the model name
                    ideaSpace.controlPanel.removeModelCardByName(finalNameToRemove);
                }

                // Clear selectedModel if we just removed it
                if (finalModelMesh == selectedModel) {
                    selectedModel = null;
                }
            });
        } else if (modelCard != null) {
            // If model doesn't exist but card does, just remove the card
            ideaSpace.controlPanel.removeModelCard(modelCard);
        }
    }

    public void unloadAllModels(String... exceptions) {
        String[] keys = loadedModels.keySet().toArray(new String[0]);

        for (String modelName : keys) {
            boolean shouldKeep = false;
            for (String exception : exceptions) {
                if (modelName.equals(exception)) {
                    shouldKeep = true;
                    break;
                }
            }

            if (!shouldKeep) {
                unloadModel(modelName, null);
            }
        }
    }

    public void reloadModel(String modelName) {
        ModelMesh modelMesh = modelLibrary.get(modelName);
        if (modelMesh != null) {
            if (loadedModels.containsKey(modelName)) unloadModel(modelName, null);
            loadModel(modelMesh);
        }
    }


    public ModelInstance getModelInstance(String name) {
        return loadedModels.get(name).getScene().modelInstance;
    }

    public void dispose() {
        for (ModelMesh modelMesh : loadedModels.values()) {
            modelMesh.getModelSceneAsset().dispose();
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

        if (scene.animationController != null) {
            scene.animationController.animate("Split", 1, 1f, null, 0f);
        } else {
            System.out.println("Model has no animation controller!");
        }
    }

    public IdeaSpace getIdeaSpace() {
        return ideaSpace;
    }

}
