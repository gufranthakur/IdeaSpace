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

    public enum Rotation {
        BOTTOM_ROTATE,
        TOP_ROTATE,
        RIGHT_ROTATE,
        LEFT_ROTATE,
        NONE_VIEW
    }

    Rotation currentRotation;

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

    private Quaternion slerpFrom = new Quaternion();
    private Quaternion slerpTo   = new Quaternion();
    private Vector3    slerpTranslation = new Vector3();
    private Vector3    slerpScale       = new Vector3();
    private float      slerpAlpha = 1f;

    private SimulationHand rightHand, leftHand;
    private HandLines rightHandLines, leftHandLines;
    private GrabHandler rightGrabHandler, leftGrabHandler;


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
        camera.position.set(-0.036988314f, 0.20096034f, 3.1463299f);
        camera.lookAt(0.008157247f, -0.18134354f, -0.9833858f);
        camera.up.set(Vector3.Y);
        camera.update();

        currentRotation = Rotation.BOTTOM_ROTATE;

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

        if (slerpAlpha < 1f && ideaSpace.modelHandler.getSelectedModel() != null) {
            slerpAlpha = Math.min(slerpAlpha + deltaTime * 5f, 1f);
            Quaternion current = new Quaternion(slerpFrom).slerp(slerpTo, slerpAlpha);
            ideaSpace.modelHandler.getSelectedModel().getScene().modelInstance
                .transform.set(slerpTranslation, current, slerpScale);
        }

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        canvasRenderer.render();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    public void switchView(Rotation rotation) {
        currentRotation = rotation;
        rotateSelectedModelToFaceView(rotation);
    }

    public void rotateSelectedModelToFaceView(Rotation rotation) {
        ModelMesh selectedModel = ideaSpace.modelHandler.getSelectedModel();
        if (selectedModel == null || selectedModel.getScene() == null) return;

        ModelInstance instance = selectedModel.getScene().modelInstance;

        instance.transform.getRotation(slerpFrom);
        instance.transform.getTranslation(slerpTranslation);
        instance.transform.getScale(slerpScale);

        Quaternion delta = new Quaternion();

        switch (rotation) {
            case BOTTOM_ROTATE:
                delta.setEulerAngles(0, 90, 0);
                break;
            case TOP_ROTATE:
                delta.setEulerAngles(0, 0, 0);
                break;
            case LEFT_ROTATE:
                delta.setEulerAngles(270, 0, 0);
                break;
            case RIGHT_ROTATE:
                delta.setEulerAngles(90, 0, 0);
                break;
            default:
                return;
        }

        slerpTo.set(slerpFrom).mul(delta);
        slerpAlpha = 0f;
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
