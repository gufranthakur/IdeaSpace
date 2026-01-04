package com.ideaspace.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class SimulationHand implements Disposable {

    public static final int LANDMARK_COUNT = 21;

    private float[] parsedData = new float[LANDMARK_COUNT * 3];

    public float sphereSize = 0.08f;
    public float handScaleX = 2.5f;
    public float handScaleY = 2.5f;
    public float handScaleZ = 3.5f;

    public float offsetForward = 2f;
    public float offsetRight = 0f;
    public float offsetUp = 0f;

    public float imgWidth = 640f;
    public float imgHeight = 480f;

    private UDPReceiver receiver;
    private Array<Scene> handScenes;
    private Model sphereModel;
    private Camera camera;
    private SceneManager sceneManager;

    private Vector3 tempForward = new Vector3();
    private Vector3 tempRight = new Vector3();
    private Vector3 tempUp = new Vector3();

    public SimulationHand(int port, Camera camera, SceneManager sceneManager) {
        this.camera = camera;
        this.sceneManager = sceneManager;
        receiver = new UDPReceiver(port);
        handScenes = new Array<>(LANDMARK_COUNT);

        createSphereModel();

        for (int i = 0; i < LANDMARK_COUNT; i++) {
            Color color = getColorForLandmark(i);
            Scene scene = createHandScene(color);
            handScenes.add(scene);
            sceneManager.addScene(scene);
        }

        receiver.start();
    }

    private void createSphereModel() {
        ModelBuilder builder = new ModelBuilder();
        sphereModel = builder.createSphere(
            sphereSize, sphereSize, sphereSize, 16, 16,
            new Material(),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );
    }

    private Scene createHandScene(Color color) {
        Scene scene = new Scene(sphereModel);
        Material mat = scene.modelInstance.materials.get(0);
        mat.set(PBRColorAttribute.createBaseColorFactor(color));
        mat.set(PBRFloatAttribute.createMetallic(0.2f));
        mat.set(PBRFloatAttribute.createRoughness(0.5f));
        return scene;
    }

    private Color getColorForLandmark(int index) {
        if (index == 0) return Color.WHITE;
        if (index <= 4) return Color.RED;
        if (index <= 8) return Color.GREEN;
        if (index <= 12) return Color.BLUE;
        if (index <= 16) return Color.YELLOW;
        return Color.MAGENTA;
    }

    public void update() {
        String data = receiver.getData();
        if (data == null || data.isEmpty()) return;

        try {
            if (data.startsWith("[")) data = data.substring(1);
            if (data.endsWith("]")) data = data.substring(0, data.length() - 1);

            String[] points = data.split(",");
            if (points.length < LANDMARK_COUNT * 3) return;

            for (int i = 0; i < LANDMARK_COUNT * 3; i++) {
                parsedData[i] = Float.parseFloat(points[i].trim());
            }

            tempForward.set(camera.direction).nor();
            tempRight.set(camera.direction).crs(camera.up).nor();
            tempUp.set(camera.up).nor();

            float bx = camera.position.x + tempForward.x * offsetForward + tempRight.x * offsetRight + tempUp.x * offsetUp;
            float by = camera.position.y + tempForward.y * offsetForward + tempRight.y * offsetRight + tempUp.y * offsetUp;
            float bz = camera.position.z + tempForward.z * offsetForward + tempRight.z * offsetRight + tempUp.z * offsetUp;

            for (int i = 0; i < LANDMARK_COUNT; i++) {
                float localX = (parsedData[i * 3] / imgWidth - 0.5f) * handScaleX;
                float localY = (0.5f - parsedData[i * 3 + 1] / imgHeight) * handScaleY;
                float localZ = (parsedData[i * 3 + 2] / 200f) * handScaleZ;

                float x = bx + tempRight.x * localX + tempUp.x * localY + tempForward.x * localZ;
                float y = by + tempRight.y * localX + tempUp.y * localY + tempForward.y * localZ;
                float z = bz + tempRight.z * localX + tempUp.z * localY + tempForward.z * localZ;

                handScenes.get(i).modelInstance.transform.setToTranslation(x, y, z);
            }
        } catch (Exception e) {}
    }

    public Vector3 getPosition(int index) {
        return handScenes.get(index).modelInstance.transform.getTranslation(new Vector3());
    }

    public Array<Scene> getHandScenes() {
        return handScenes;
    }

    @Override
    public void dispose() {
        receiver.stop();
        sphereModel.dispose();
        for (Scene scene : handScenes) {
            sceneManager.removeScene(scene);
        }
    }
}
