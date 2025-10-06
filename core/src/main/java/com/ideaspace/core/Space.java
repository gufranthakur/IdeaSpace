package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.ideaspace.IdeaSpace;

import com.ideaspace.components.Panel;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.ArrayList;

public class Space {

    private IdeaSpace ideaSpace;

    private SceneManager sceneManager;
    public PerspectiveCamera camera;

    private Cubemap diffuseCubeMap, environmentCubeMap, specularCubeMap;
    private Texture brdfLUT;

    private float time;
    private SceneSkybox skybox;
    private DirectionalLightEx light;;
    private FirstPersonCameraController cameraController;

    public ArrayList<Panel> panels;
    public Panel selectedPanel;

    public float cameraMoveRemaining = 0f;   // total distance left to move
    public float cameraMoveSpeed = 1.5f;     // units per second

    public Space(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        sceneManager = new SceneManager();
        panels = new ArrayList<>();

        setupCamera();
        setupLighting();
        setupIBL();
        setupSceneManager();
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 20f / 1000f;
        camera.far = 80f;
        sceneManager.setCamera(camera);
        camera.position.set(0,0.5f, 4f);

        cameraController = new FirstPersonCameraController(camera);
        Gdx.input.setInputProcessor(cameraController);
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
        sceneManager.setAmbientLight(1f);
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubeMap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubeMap));

        skybox = new SceneSkybox(environmentCubeMap);
        sceneManager.setSkyBox(skybox);
    }

    public void render(float deltaTime) {
        time += deltaTime;
        cameraController.update();

        float delta = Gdx.graphics.getDeltaTime();

        if (Math.abs(cameraMoveRemaining) > 0.0001f) {
            float direction = Math.signum(cameraMoveRemaining);
            float move = direction * cameraMoveSpeed * delta;

            if (Math.abs(move) > Math.abs(cameraMoveRemaining)) {
                move = cameraMoveRemaining;
            }

            camera.translate(0, 0, move);
            cameraMoveRemaining -= move;

            if (Math.abs(cameraMoveRemaining) < 0.0001f) {
                cameraMoveRemaining = 0f;
            }
        }

    }

    public void dispose() {
        environmentCubeMap.dispose();
        diffuseCubeMap.dispose();
        specularCubeMap.dispose();
        brdfLUT.dispose();
        skybox.dispose();

        for (Panel panel : panels) {
            for (SceneAsset sceneAsset : panel.getObjectAssets().values()) {
                sceneAsset.dispose();
            }
        }
    }


    public void addPanel() {
        Panel panel = new Panel(this);
        panels.add(panel);

        selectedPanel = panel;
    }


    public SceneManager getSceneManager() {
        return sceneManager;
    }

}
