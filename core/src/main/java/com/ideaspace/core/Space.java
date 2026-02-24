package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.utils.FirstPersonCameraController;

import com.badlogic.gdx.math.Vector3;
import com.ideaspace.IdeaSpace;
import com.ideaspace.handlers.GrabHandler;

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
        FRONT_VIEW ,
        TOP_VIEW,
        RIGHT_VIEW,
        LEFT_VIEW
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
        camera.position.set(0f,2.0f, 3.0f); // Top view (0.024993863,2.5312417,0.42255044)
        camera.lookAt(0f, 0.5f, 0f); //Direction (-0.0012064216,-0.9904221,-0.13806608)
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

        Gdx.gl.glDisable(GL20.GL_DEPTH_TEST);
        canvasRenderer.render();
        Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    }

    //top view
    // Pos: (0.12647918,2.8108501,0.77340674)
    //Dir: (0.0024682502,-0.9591379,-0.28292692)

    //right view
    // Pos: (4.383636,0.16763468,0.026943728)
    // Dir: (-0.9985318,-0.05345491,0.008718704)

    //left view
    //Pos: (-3.684233,0.088000536,-0.040506694)
    //Dir: (0.9778776,-0.20899758,-0.008555933)

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

    public void switchView(View view) {

        switch (view) {
            case View.TOP_VIEW : {
                camera.position.set(0.1624249f,3.985626f,-0.030935928f);
                camera.lookAt (-6.4930646E-7f,-0.99820155f,-0.059939645f);
                camera.up.set(Vector3.Y);
                camera.update();
            } break;

            case View.RIGHT_VIEW : {
                camera.position.set(4.383636f ,0.16763468f ,0.026943728f);
                camera.lookAt(-0.9985318f ,-0.05345491f ,0.008718704f );
                camera.up.set(Vector3.Y);
                camera.update();
            } break;

            case View.LEFT_VIEW : {
                camera.position.set(-3.684233f ,0.088000536f ,-0.040506694f );
                camera.lookAt(-0.0012064f, -0.990f, -0.13806608f);
                camera.up.set(Vector3.Y);
                camera.update();
            } break;

            case FRONT_VIEW : {
                camera.position.set(0f,2.0f, 3.0f); // Top view (0.024993863,2.5312417,0.42255044)
                camera.lookAt(0f, 0.5f, 0f); //Direction (-0.0012064216,-0.9904221,-0.13806608)
                camera.up.set(Vector3.Y);
                camera.update();
            } break;


        }

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
