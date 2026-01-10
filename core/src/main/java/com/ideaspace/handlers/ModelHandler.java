package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.math.collision.BoundingBox;
import com.badlogic.gdx.physics.bullet.collision.btBoxShape;
import com.badlogic.gdx.physics.bullet.collision.btCollisionShape;
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
        //createModel("Background", "models/backgrounds/dark_background.glb");
        //loadModel(modelLibrary.get("Background"), false);
        //getModelInstance("Background").transform.idt().scale(50f, 50f, 50f);
    }

    public void createModels() {
        createModel("Esp32", "models/microcontrollers/esp32.glb");
        createModel("mechanical_keyboard", "models/misc/mechanical_keyboard.glb");
        createModel("Drone", "models/misc/cp_drone.glb");
        createModel("Iphone 17", "models/misc/iphone17pro.glb");
    }

    private void createModel(String name, String path) {
        ModelMesh modelMesh = new ModelMesh(name, path);
        modelLibrary.put(name, modelMesh);

        if (name.equals("Background")) return;

        ModelCard modelCard = new ModelCard(this, modelMesh, false);
        ideaSpace.modelControlPanel.addModelCardToLibrary(modelCard);
    }

    public void loadModel(ModelMesh modelMesh) {
        loadModel(modelMesh, true); // Default: enable physics
    }

    public void loadModel(ModelMesh modelMesh, boolean enablePhysics) {
        if (loadedModels.containsKey(modelMesh.modelName)) {
            System.out.println("Model already exists!");
            return;
        }

        SceneAsset sceneAsset = new GLBLoader().load(Gdx.files.internal(modelMesh.modelPath));
        Scene scene = new Scene(sceneAsset.scene);

        modelMesh.setScene(scene);
        modelMesh.setModelSceneAsset(sceneAsset);

        // Add physics if enabled
        if (enablePhysics) {
            // Calculate bounding box
            BoundingBox bounds = new BoundingBox();
            scene.modelInstance.calculateBoundingBox(bounds);
            Vector3 dimensions = bounds.getDimensions(new Vector3()).scl(0.5f);

            // Create collision shape
            btCollisionShape shape = new btBoxShape(dimensions);

            // Get current position
            Vector3 position = new Vector3();
            scene.modelInstance.transform.getTranslation(position);

            // Create physics body
            modelMesh.createPhysicsBody(shape, 1f, position);
            ideaSpace.space.addRigidBody(modelMesh.getRigidBody());
        }

        loadedModels.put(modelMesh.modelName, modelMesh);
        ideaSpace.space.getSceneManager().addScene(modelMesh.getScene());

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
                // Remove from physics world if it has a rigid body
                if (finalModelMesh.getRigidBody() != null) {
                    ideaSpace.space.removeRigidBody(finalModelMesh.getRigidBody());
                }

                ideaSpace.space.getSceneManager().removeScene(finalModelMesh.getScene());
                finalModelMesh.dispose(); // This will dispose physics components
                loadedModels.remove(finalNameToRemove);

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
            modelMesh.dispose(); // Now disposes both model and physics
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
        loadModel(randomModel, true); // Enable physics for random models
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

    public ModelMesh getSelectedModel() {
        return selectedModel;
    }

    public IdeaSpace getIdeaSpace() {
        return ideaSpace;
    }
}
