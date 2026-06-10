package com.ideaspace.handlers;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.model.Node;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.ideaspace.IdeaSpace;
import com.ideaspace.models.ModelMesh;
import com.ideaspace.models.SplitPiece;
import com.ideaspace.ui.components.ModelCard;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserAdapter;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.HashMap;

public class ModelHandler {

    private IdeaSpace ideaSpace;
    private ModelMesh selectedModel;

    public HashMap<String, ModelMesh> loadedModels;
    public HashMap<String, ModelMesh> modelLibrary;
    public HashMap<String, SplitPiece> splitPieces;
    public ArrayList<ModelMesh> maps;
    private int mapIndex = 0;

    public ModelHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        loadedModels = new HashMap<>();
        modelLibrary = new HashMap<>();
        splitPieces  = new HashMap<>();
        maps = new ArrayList<>();
    }

    public void loadInitialModels() {
        addModelToLibrary("Dark", "models/backgrounds/dark_background.glb", maps);

        addModelToLibrary("Spaceship", "models/backgrounds/spaceship.glb", maps);

        loadModel(modelLibrary.get("Dark"));
        getModelInstance("Dark").transform.idt().scale(30f, 30f, 30f);
    }

    public void createModels() {
        addModelToLibrary("Arduino-Uno", "models/microcontrollers/arduino_uno2222.glb");
        addModelToLibrary("Esp32", "models/microcontrollers/esp32.glb");
        addModelToLibrary("Iphone-17", "models/misc/iphone17pro.glb");
        addModelToLibrary("Joystick-Module", "models/components/joystick_module.glb");
        addModelToLibrary("Servo-Motor", "models/components/servomotor.glb");
        addModelToLibrary("l298motordriver", "models/components/l298motordriver.glb");
        addModelToLibrary("RaspberryPi", "models/microcontrollers/rpi4.glb");
        addModelToLibrary("Rpi-cam", "models/components/rpicamera_split.glb");
        addModelToLibrary("SD-Card Module", "models/components/sdcard.glb");
        addModelToLibrary("DC Motor", "models/components/dcmotor.glb");
        addModelToLibrary("DDR4", "models/components/ddr4.glb");
        addModelToLibrary("Laptop Fan", "models/components/laptop_fan.glb");
        addModelToLibrary("Mechanical-Keyboard", "models/misc/mechanicalkeyboard_split.glb");
        addModelToLibrary("Servo Process", "models/components/servo_working.glb");
        addModelToLibrary("Drone", "models/misc/drone_split.glb");
        addModelToLibrary("Controller", "models/misc/controller.glb");
        addModelToLibrary("Controller", "models/misc/controller.glb");

        loadModel(modelLibrary.get("Iphone-17"));
    }

    private void addModelToLibrary(String name, String path) {
        ModelMesh modelMesh = new ModelMesh(name, path);
        modelLibrary.put(name, modelMesh);
        if (isBackground(name)) return;
        ModelCard modelCard = new ModelCard(this, modelMesh, false);
        ideaSpace.controlPanel.addModelCardToLibrary(modelCard);
    }

    private void addModelToLibrary(String name, String path, ArrayList<ModelMesh> maps) {
        ModelMesh modelMesh = new ModelMesh(name, path);
        modelLibrary.put(name, modelMesh);
        maps.add(modelMesh);
        if (isBackground(name)) return;
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

        // Spawn animation — skip for background maps
        if (!isBackground(modelMesh.modelName)) {
            ideaSpace.animationHandler.spawnModelAnimation(scene.modelInstance);
        }

        ideaSpace.space.getRightGrabHandler().setLoadedModels(loadedModels.values());
        ideaSpace.space.getLeftGrabHandler().setLoadedModels(loadedModels.values());

        if (isBackground(modelMesh.modelName)) return;

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
            final String finalNameToRemove = nameToRemove;
            final ModelMesh finalModelMesh = modelMesh;

            ideaSpace.animationHandler.removeModelAnimation(modelInstance, () -> {
                ideaSpace.space.getSceneManager().removeScene(finalModelMesh.getScene());
                finalModelMesh.dispose();
                loadedModels.remove(finalNameToRemove);

                ideaSpace.space.getRightGrabHandler().setLoadedModels(loadedModels.values());

                if (modelCard != null) {
                    ideaSpace.controlPanel.removeModelCard(modelCard);
                } else {
                    ideaSpace.controlPanel.removeModelCardByName(finalNameToRemove);
                }

                if (finalModelMesh == selectedModel) {
                    selectedModel = null;
                }
            });
        } else if (modelCard != null) {
            ideaSpace.controlPanel.removeModelCard(modelCard);
        }
    }

    public void breakdown() {
        if (selectedModel == null) {
            System.out.println("[BREAKDOWN] No model selected, aborting.");
            return;
        }

        System.out.println("[BREAKDOWN] Starting breakdown of: " + selectedModel.modelName);

        clearBreakdown();

        com.badlogic.gdx.graphics.g3d.Model rawModel =
            selectedModel.getModelSceneAsset().scene.model;

        System.out.println("[BREAKDOWN] Total nodes in raw model: " + rawModel.nodes.size);

        ideaSpace.space.getSceneManager().removeScene(selectedModel.getScene());
        System.out.println("[BREAKDOWN] Original model hidden.");

        int pieceCount = 0;

        for (Node node : rawModel.nodes) {
            if (node.parts.size == 0) {
                System.out.println("[BREAKDOWN] Skipping node with no parts: " + node.id);
                continue;
            }

            ModelInstance pieceInstance = new ModelInstance(rawModel, node.id);
            Scene pieceScene = new Scene(pieceInstance);
            SplitPiece piece = new SplitPiece(node.id, selectedModel.modelName, pieceScene);

            splitPieces.put(node.id, piece);
            ideaSpace.space.getSceneManager().addScene(pieceScene);

            System.out.println("[BREAKDOWN] Piece added: " + node.id);
            pieceCount++;
        }

        ideaSpace.space.getRightGrabHandler().setSplitPieces(splitPieces.values());
        ideaSpace.space.getLeftGrabHandler().setSplitPieces(splitPieces.values());

        System.out.println("[BREAKDOWN] Done. Total pieces: " + pieceCount);
    }

    public void clearBreakdown() {
        if (splitPieces.isEmpty()) return;

        System.out.println("[BREAKDOWN] Clearing " + splitPieces.size() + " pieces.");

        for (SplitPiece piece : splitPieces.values()) {
            ideaSpace.space.getSceneManager().removeScene(piece.getScene());
        }
        splitPieces.clear();

        ideaSpace.space.getRightGrabHandler().setSplitPieces(null);
        ideaSpace.space.getLeftGrabHandler().setSplitPieces(null);

        if (selectedModel != null && selectedModel.getScene() != null) {
            ideaSpace.space.getSceneManager().addScene(selectedModel.getScene());
            System.out.println("[BREAKDOWN] Original model restored.");
        }
    }

    public void splitModel() {
        if (selectedModel == null) {
            System.out.println("No model selected!");
            return;
        }

        if (selectedModel.isSplit) {
            unsplitModel();
            return;
        }

        selectedModel.isSplit = true;


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

    public void unsplitModel() {
        if (selectedModel == null) {
            System.out.println("No model selected!");
            return;
        }

        Scene scene = selectedModel.getScene();
        if (scene.animationController == null) return;

        String[] names = { "split", "Split" };

        for (String name : names) {
            try {
                scene.animationController.animate(name, 1, -1f, null, 0f);
                //                                        ^    ^^^
                //                                        |    negative speed = reverse
                //                                        1 play count
                selectedModel.isSplit = false;
                return;
            } catch (GdxRuntimeException ignored) {}
        }



        System.out.println("Model has no split animation");
    }

    public void changeMap() {
        String currentMap = maps.get(mapIndex).modelName;
        mapIndex = (mapIndex + 1) % maps.size();

        ModelMesh nextMap = maps.get(mapIndex);
        loadModel(nextMap);

        if (nextMap.modelName.equals("Spaceship")) {
            getModelInstance("Spaceship").transform.idt()
                .scale(5f, 5f, 5f)
                .translate(0.3f, -0.75f, 0.6f);
        } else if (nextMap.modelName.equals("Light")) {
            getModelInstance("Light").transform.idt().scale(30f, 30f, 30f);
        } else if (nextMap.modelName.equals("Dark")) {
            getModelInstance("Dark").transform.idt().scale(30f, 30f, 30f);
        }

        unloadModel(currentMap, null);
    }

    public void loadRandomModel() {
        if (modelLibrary.isEmpty()) return;

        java.util.List<String> availableModels = new java.util.ArrayList<>();
        for (String modelName : modelLibrary.keySet()) {
            if (!isBackground(modelName)) availableModels.add(modelName);
        }

        if (availableModels.isEmpty()) return;

        int randomIndex = (int) (Math.random() * availableModels.size());
        String randomModelName = availableModels.get(randomIndex);

        ModelMesh randomModel = modelLibrary.get(randomModelName);
        loadModel(randomModel);
        selectedModel = randomModel;
    }

    public void loadCustomModel() {
        String os = System.getProperty("os.name").toLowerCase();
        boolean isMac     = os.contains("mac");
        boolean isWindows = os.contains("win");
        boolean isLinux   = os.contains("nux") || os.contains("nix");

        if (isMac)     loadCustomModelMac();
        if (isWindows) loadCustomModelWindows();
        if (isLinux)   loadCustomModelLinux();
    }

    public void loadCustomModelMac() {
        Thread thread = new Thread(() -> {
            try {
                String[] cmd = {
                    "osascript", "-e",
                    "POSIX path of (choose file of type {\"glb\"} with prompt \"Select a GLB Model\")"
                };
                Process process = Runtime.getRuntime().exec(cmd);
                String filePath = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor();
                if (!filePath.isEmpty()) {
                    Gdx.app.postRunnable(() -> addModelToLibrary(
                        filePath.substring(filePath.lastIndexOf("/") + 1, filePath.lastIndexOf(".")),
                        filePath
                    ));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void loadCustomModelWindows() {
        Thread thread = new Thread(() -> {
            try {
                String[] cmd = {
                    "powershell", "-Command",
                    "Add-Type -AssemblyName System.Windows.Forms;" +
                        "$f = New-Object System.Windows.Forms.OpenFileDialog;" +
                        "$f.Filter = 'GLB Files (*.glb)|*.glb';" +
                        "$f.Title = 'Select a GLB Model';" +
                        "if ($f.ShowDialog() -eq 'OK') { $f.FileName }"
                };
                Process process = Runtime.getRuntime().exec(cmd);
                String filePath = new String(process.getInputStream().readAllBytes()).trim();
                process.waitFor();
                if (!filePath.isEmpty()) {
                    Gdx.app.postRunnable(() -> System.out.println(filePath));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.setDaemon(true);
        thread.start();
    }

    public void loadCustomModelLinux() {
        FileChooser fileChooser = new FileChooser(FileChooser.Mode.OPEN);
        fileChooser.setSelectionMode(FileChooser.SelectionMode.FILES);
        ideaSpace.hudPanel.getStage().addActor(fileChooser.fadeIn());
        fileChooser.setListener(new FileChooserAdapter() {
            @Override
            public void selected(Array<FileHandle> files) {
                if (!files.isEmpty()) {
                    FileHandle file = files.first();
                    System.out.println("Selected file: " + file.path());
                }
            }
        });
    }

    public ModelInstance getModelInstance(String name) {
        return loadedModels.get(name).getScene().modelInstance;
    }

    public boolean isBackground(String name) {
        return name.equals("Spaceship") || name.equals("Light") || name.equals("Dark");
    }

    public void setSelectedModel(ModelMesh model) { this.selectedModel = model; }
    public ModelMesh getSelectedModel()            { return selectedModel; }
    public IdeaSpace getIdeaSpace()                { return ideaSpace; }

    public void dispose() {
        clearBreakdown();
        for (ModelMesh modelMesh : loadedModels.values()) {
            modelMesh.dispose();
        }
    }
}
