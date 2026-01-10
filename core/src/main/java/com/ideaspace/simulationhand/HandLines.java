package com.ideaspace.simulationhand;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;
import net.mgsx.gltf.scene3d.attributes.PBRFloatAttribute;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneManager;

public class HandLines implements Disposable {

    private static final int[][] CONNECTIONS = {
        {0, 1}, {0, 5}, {0, 9}, {0, 13}, {0, 17},
        {1, 2}, {2, 3}, {3, 4},
        {5, 6}, {6, 7}, {7, 8},
        {9, 10}, {10, 11}, {11, 12},
        {13, 14}, {14, 15}, {15, 16},
        {17, 18}, {18, 19}, {19, 20},
        {5, 9}, {9, 13}, {13, 17}
    };

    private Model cylinderModel;
    private Array<Scene> lineScenes;
    private SceneManager sceneManager;

    public float thickness = 0.02f;
    public Color lineColor = Color.WHITE;

    private Vector3 start = new Vector3();
    private Vector3 end = new Vector3();
    private Vector3 dir = new Vector3();
    private Vector3 mid = new Vector3();
    private Vector3 yAxis = new Vector3(0, 1, 0);
    private Vector3 axis = new Vector3();
    private Quaternion rotation = new Quaternion();

    public HandLines(SceneManager sceneManager) {
        this.sceneManager = sceneManager;

        ModelBuilder builder = new ModelBuilder();
        cylinderModel = builder.createCylinder(
            thickness, 1f, thickness, 8,
            new Material(),
            VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal
        );

        lineScenes = new Array<>(CONNECTIONS.length);
        for (int i = 0; i < CONNECTIONS.length; i++) {
            Scene scene = new Scene(cylinderModel);
            Material mat = scene.modelInstance.materials.get(0);
            mat.set(PBRColorAttribute.createBaseColorFactor(lineColor));
            mat.set(PBRFloatAttribute.createMetallic(0.1f));
            mat.set(PBRFloatAttribute.createRoughness(0.7f));
            lineScenes.add(scene);
            sceneManager.addScene(scene);
        }
    }

    public void update(SimulationHand hand) {
        for (int i = 0; i < CONNECTIONS.length; i++) {
            start.set(hand.getPosition(CONNECTIONS[i][0]));
            end.set(hand.getPosition(CONNECTIONS[i][1]));

            dir.set(end).sub(start);
            float length = dir.len();

            if (length < 0.0001f) continue;

            dir.nor();
            mid.set(start).add(end).scl(0.5f);

            // Calculate rotation from Y-axis to direction
            axis.set(yAxis).crs(dir);
            float dot = yAxis.dot(dir);

            if (axis.len2() > 0.0001f) {
                float angle = (float) Math.toDegrees(Math.acos(Math.min(1f, Math.max(-1f, dot))));
                rotation.setFromAxis(axis.nor(), angle);
            } else if (dot < 0) {
                // Pointing opposite direction
                rotation.setFromAxis(1, 0, 0, 180);
            } else {
                rotation.idt();
            }

            Matrix4 transform = lineScenes.get(i).modelInstance.transform;
            transform.idt();
            transform.translate(mid);
            transform.rotate(rotation);
            transform.scale(1f, length, 1f);
        }
    }

    @Override
    public void dispose() {
        cylinderModel.dispose();
        for (Scene scene : lineScenes) {
            sceneManager.removeScene(scene);
        }
    }
}
