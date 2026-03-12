package com.ideaspace.simulationhand;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class CursorOverlay {

    private ShapeRenderer shapeRenderer;
    private OrthographicCamera orthoCamera;
    private UDPReceiver receiver;

    private float cursorX    = -1;
    private float cursorY    = -1;
    private boolean menuOpen = true;

    private static final float CAM_W = 640f;
    private static final float CAM_H = 480f;
    private static final float DOT_RADIUS  = 12f;
    private static final float RING_RADIUS = 22f;

    private int lastW = 0, lastH = 0;

    public CursorOverlay(int port) {
        shapeRenderer = new ShapeRenderer();
        orthoCamera = new OrthographicCamera();
        receiver = new UDPReceiver(port);
        receiver.start();
    }

    public void update() {
        String data = receiver.getData();
        if (data == null || !data.startsWith("CURSOR:")) return;
        try {
            String[] parts = data.replace("CURSOR:", "").split(",");
            cursorX  = Float.parseFloat(parts[0].trim());
            cursorY  = Float.parseFloat(parts[1].trim());
            menuOpen = parts[2].trim().equals("1");
        } catch (Exception ignored) {}
    }

    public void render() {
        if (!menuOpen || cursorX < 0 || cursorY < 0) return;

        int screenW = Gdx.graphics.getWidth();
        int screenH = Gdx.graphics.getHeight();

        // Only recalculate projection when screen size changes
        if (screenW != lastW || screenH != lastH) {
            orthoCamera.setToOrtho(false, screenW, screenH);
            orthoCamera.update();
            lastW = screenW;
            lastH = screenH;
        }

        float sx = (cursorX / CAM_W) * screenW;
        float sy = (1f - cursorY / CAM_H) * screenH;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        shapeRenderer.setProjectionMatrix(orthoCamera.combined);

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(new Color(0f, 1f, 0.71f, 0.95f));
        shapeRenderer.circle(sx, sy, DOT_RADIUS);
        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        Gdx.gl.glLineWidth(2f);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(sx, sy, RING_RADIUS);
        shapeRenderer.end();

        Gdx.gl.glDisable(GL20.GL_BLEND);
        Gdx.gl.glLineWidth(1f);
    }

    public boolean isMenuOpen() { return menuOpen; }

    public void dispose() {
        receiver.stop();
        shapeRenderer.dispose();
    }
}
