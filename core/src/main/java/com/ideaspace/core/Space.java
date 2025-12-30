package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Cubemap;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;
import com.ideaspace.IdeaSpace;

import com.ideaspace.models.ModelMesh;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.attributes.PBRCubemapAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRTextureAttribute;
import net.mgsx.gltf.scene3d.lights.DirectionalLightEx;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;
import net.mgsx.gltf.scene3d.scene.SceneManager;
import net.mgsx.gltf.scene3d.scene.SceneSkybox;
import net.mgsx.gltf.scene3d.utils.IBLBuilder;

import java.util.HashMap;
import java.util.Iterator;
import com.badlogic.gdx.graphics.GL20;
public class Space {

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

    private Iterator<String> modelIterator;
    private String currentModel;

    public Space(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        sceneManager = new SceneManager();

        setupCamera();
        setupLighting();
        setupIBL();
        setupSceneManager();

        canvasRenderer = new CanvasRenderer(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(60f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        camera.near = 20f / 1000f;
        camera.far = 150;
        sceneManager.setCamera(camera);
        camera.position.set(0,0.5f, 4f);

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
        sceneManager.setAmbientLight(3f); // Reduce from 3f for better PBR rendering
        sceneManager.environment.set(new PBRTextureAttribute(PBRTextureAttribute.BRDFLUTTexture, brdfLUT));
        //sceneManager.environment.set(PBRCubemapAttribute.createSpecularEnv(specularCubeMap));
        sceneManager.environment.set(PBRCubemapAttribute.createDiffuseEnv(diffuseCubeMap));

        skybox = new SceneSkybox(environmentCubeMap);
        sceneManager.setSkyBox(skybox); // Uncomment this too for visual context
    }

    public void render(float deltaTime) {
        time += deltaTime;
        cameraController.update();

        ideaSpace.decoder.update(deltaTime);

        camera.update();
        sceneManager.update(deltaTime);
        sceneManager.render();

        // Disable depth test for 2D overlay
        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        canvasRenderer.render();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }



    public void addObject(ModelMesh modelMesh) {
        getSceneManager().addScene(modelMesh.getScene());
    }




    public void dispose() {
        environmentCubeMap.dispose();
        diffuseCubeMap.dispose();
        specularCubeMap.dispose();
        brdfLUT.dispose();
        skybox.dispose();

        canvasRenderer.dispose();

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






