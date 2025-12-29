package com.ideaspace.core;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class CanvasRenderer {

    private ArrayList<CanvasStroke> strokes;
    private CanvasStroke currentStroke;
    private ShapeRenderer shapeRenderer;
    private int screenWidth;
    private int screenHeight;

    public CanvasRenderer(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.strokes = new ArrayList<>();
        this.shapeRenderer = new ShapeRenderer();
        this.shapeRenderer.setAutoShapeType(true);
    }

    public void startStroke(float normX, float normY, int r, int g, int b, int thickness, boolean isEraser) {
        currentStroke = new CanvasStroke(r, g, b, thickness, isEraser);
        addPoint(normX, normY, r, g, b, thickness);
    }

    public void addPoint(float normX, float normY, int r, int g, int b, int thickness) {
        if (currentStroke == null) {
            startStroke(normX, normY, r, g, b, thickness, false);
        }

        float screenX = normX * screenWidth;
        float screenY = (1.0f - normY) * screenHeight;

        currentStroke.addPoint(screenX, screenY);
    }

    public void endStroke() {
        if (currentStroke != null && currentStroke.points.size() > 0) {
            strokes.add(currentStroke);
            currentStroke = null;
        }
    }

    public void clearCanvas() {
        strokes.clear();
        currentStroke = null;
    }

    public void render() {
        if (strokes.isEmpty() && currentStroke == null) return;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(
            new com.badlogic.gdx.math.Matrix4().setToOrtho2D(0, 0, screenWidth, screenHeight)
        );

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        for (CanvasStroke stroke : strokes) {
            stroke.render(shapeRenderer);
        }

        if (currentStroke != null) {
            currentStroke.render(shapeRenderer);
        }

        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
    }

    public void dispose() {
        shapeRenderer.dispose();
    }

    public void resize(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
    }

    private static class CanvasStroke {
        ArrayList<Vector2> points;
        Color color;
        int thickness;
        boolean isEraser;

        public CanvasStroke(int r, int g, int b, int thickness, boolean isEraser) {
            this.points = new ArrayList<>();
            this.color = new Color(r / 255f, g / 255f, b / 255f, 1f);
            this.thickness = thickness;
            this.isEraser = isEraser;
        }

        public void addPoint(float x, float y) {
            points.add(new Vector2(x, y));
        }

        public void render(ShapeRenderer renderer) {
            if (points.size() < 2) return;

            renderer.setColor(isEraser ? Color.BLACK : color);

            float drawThickness = thickness * 4;

            for (int i = 0; i < points.size() - 1; i++) {
                Vector2 p1 = points.get(i);
                Vector2 p2 = points.get(i + 1);

                renderer.rectLine(p1.x, p1.y, p2.x, p2.y, drawThickness);
                renderer.circle(p1.x, p1.y, drawThickness / 2);
            }

            if (points.size() > 0) {
                Vector2 lastPoint = points.get(points.size() - 1);
                renderer.circle(lastPoint.x, lastPoint.y, drawThickness / 2);
            }
        }
    }
}
