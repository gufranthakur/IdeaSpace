package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;

import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.ideaspace.IdeaSpace;
import com.ideaspace.handlers.GrabHandler;

import com.ideaspace.models.ModelMesh;
import com.ideaspace.simulationhand.HandLines;
import com.ideaspace.simulationhand.SimulationHand;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

public class Space {

    public enum View {
        FRONT_VIEW,
        TOP_VIEW,
        RIGHT_VIEW,
        LEFT_VIEW,
        NONE_VIEW
    }

    View currentView;

    private IdeaSpace ideaSpace;

    private SceneManager sceneManager;
    public PerspectiveCamera camera;

    private Cubemap diffuseCubeMap, environmentCubeMap, specularCubeMap;
    private Texture brdfLUT;

    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;
    private FirstPersonCameraController cameraController;

    public CanvasRenderer canvasRenderer;

    private SimulationHand rightHand, leftHand;
    private HandLines rightHandLines, leftHandLines;
    private GrabHandler rightGrabHandler, leftGrabHandler;

    private Quaternion currentViewRotation = new Quaternion();
    private Quaternion targetViewRotation  = new Quaternion();
    private boolean isSmoothRotating = false;
    private float viewRotationLerpFactor = 0.1f;


    public Space(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        sceneManager = new SceneManager();

        setupCamera();
        setupLighting();
        setupIBL();
        setupSceneManager();

        rightHand = new SimulationHand(65000, camera, sceneManager);
        leftHand = new SimulationHand(65005, camera, sceneManager);

        rightGrabHandler = new GrabHandler(rightHand, camera, true);
        leftGrabHandler = new GrabHandler(leftHand, camera, false);

        rightHandLines = new HandLines(sceneManager);
        leftHandLines = new HandLines(sceneManager);

        canvasRenderer = new CanvasRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(80f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 20f / 1000f;
        camera.far = 1000f;
        sceneManager.setCamera(camera);
        camera.position.set(0f, 2.0f, 3.0f);
        camera.lookAt(0f, 0.5f, 0f);
        camera.up.set(Vector3.Y);
        camera.update();

        currentView = View.FRONT_VIEW;

        cameraController = new FirstPersonCameraController(camera);
    }

    private void setupLighting() {
        light = new DirectionalLightEx();
        light.direction.set(1, -3, 1).nor();
        light.color.set(Color.WHITE);

        sceneManager.environment.add(light);
    }

    private void setupIBL() {
        IBLBuilder builder = IBLBuilder.createOutdoor(light);
        environmentCubeMap = builder.buildEnvMap(1024);
        diffuseCubeMap = builder.buildIrradianceMap(256);
        specularCubeMap = builder.buildRadianceMap(10);

        builder.dispose();

        brdfLUT = new Texture(Gdx.files.classpath("net/mgsx/gltf/shaders/brdfLUT.png"));
    }

    private void setupSceneManager() {
        sceneManager.setAmbientLight(3f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubeMap));

        skybox = new SceneSkybox(environmentCubeMap);
        sceneManager.setSkyBox(skybox);
    }

    public GrabHandler getRightGrabHandler() {
        return rightGrabHandler;
    }

    public GrabHandler getLeftGrabHandler() {
        return leftGrabHandler;
    }

    public void render(float deltaTime) {
        time += deltaTime;
        cameraController.update();
        ideaSpace.decoder.update(deltaTime);
        camera.update();

        rightHand.update();
        leftHand.update();

        rightGrabHandler.update();
        leftGrabHandler.update();
        rightHandLines.update(rightHand);
        leftHandLines.update(leftHand);

        sceneManager.update(deltaTime);
        sceneManager.render();

        if (!isSmoothRotating || ideaSpace.modelHandler.getSelectedModel() == null) {
            return;
        }

        ModelInstance instance = ideaSpace.modelHandler.getSelectedModel().getScene().modelInstance;
        Vector3 translation = instance.transform.getTranslation(new Vector3());
        Vector3 scale       = instance.transform.getScale(new Vector3());

        instance.transform.getRotation(currentViewRotation);
        currentViewRotation.slerp(targetViewRotation, viewRotationLerpFactor);

        if (currentViewRotation.dot(targetViewRotation) > 0.9999f) {
            currentViewRotation.set(targetViewRotation);
            isSmoothRotating = false;
        }

        instance.transform.set(translation, currentViewRotation, scale);

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        canvasRenderer.render();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    public void switchView(View view) {
        currentView = view;
        rotateSelectedModelToFaceView(view);
    }

    public void rotateSelectedModelToFaceView(Space.View view) {

        ModelMesh selectedModel = ideaSpace.modelHandler.getSelectedModel();

        if (selectedModel == null || selectedModel.getScene() == null) return;

        ModelInstance instance = selectedModel.getScene().modelInstance;

        switch (view) {
            case FRONT_VIEW:
                instance.transform.rotate(Vector3.X, 90f);
                break;
            case TOP_VIEW:
                instance.transform.rotate(Vector3.X, -90f);
                break;
            case LEFT_VIEW:
                instance.transform.rotate(Vector3.Y, -90f);
                break;
            case RIGHT_VIEW:
                instance.transform.rotate(Vector3.Y, 90f);
                break;
        }
    }

    public void dispose() {
        environmentCubeMap.dispose();
        diffuseCubeMap.dispose();
        specularCubeMap.dispose();
        brdfLUT.dispose();
        skybox.dispose();
        rightHand.dispose();
        rightHandLines.dispose();
        canvasRenderer.dispose();
        rightGrabHandler.dispose();
    }

    public void resize(int width, int height) {
        canvasRenderer.resize(width, height);
    }

    public FirstPersonCameraController getCameraController() {
        return cameraController;
    }

    public SceneManager getSceneManager() {
        return sceneManager;
    }
}
