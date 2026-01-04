package com.ideaspace.core;

import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Environment;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;

public class HandLines implements Disposable {

    private ShapeRenderer shapeRenderer;
    private Camera camera;

    // Connections: [from, to] pairs
    private static final int[][] CONNECTIONS = {
        // Wrist to finger bases
        {0, 1}, {0, 5}, {0, 9}, {0, 13}, {0, 17},
        // Thumb
        {1, 2}, {2, 3}, {3, 4},
        // Index
        {5, 6}, {6, 7}, {7, 8},
        // Middle
        {9, 10}, {10, 11}, {11, 12},
        // Ring
        {13, 14}, {14, 15}, {15, 16},
        // Pinky
        {17, 18}, {18, 19}, {19, 20},
        // Palm connections
        {5, 9}, {9, 13}, {13, 17}
    };

    public float lineWidth = 2f;
    public Color lineColor = Color.WHITE;

    public HandLines(Camera camera) {
        this.camera = camera;
        shapeRenderer = new ShapeRenderer();
    }

    public void render(Array<HandSphere> handPoints, Environment environment) {
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(lineColor);

        for (int[] conn : CONNECTIONS) {
            HandSphere from = handPoints.get(conn[0]);
            HandSphere to = handPoints.get(conn[1]);
            shapeRenderer.line(from.getPosition(), to.getPosition());
        }

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}
