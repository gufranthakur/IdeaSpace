package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Server;
import com.ideaspace.core.Space;

public class IdeaSpace extends ApplicationAdapter {
    public Space space;
    private Server server;
    public Decoder decoder;
    public Thread serverThread;

    @Override
    public void create() {
        space = new Space(this);
        server = new Server(this);
        decoder = new Decoder(this);

        space.addPanel();

        space.selectedPanel.loadObject("Blue Env", "models/environments/blue_cube.glb");
        space.selectedPanel.loadObject("ESP32", "models/microcontrollers/esp.glb");
        space.selectedPanel.loadObject("3D Printer", "models/misc/3d_printer.glb");

        space.selectedPanel.getModelInstanceOf("Blue Env").transform.idt()
            .scale(3f, 2f, 2f)
                .translate(-53f, -55f, 0f);

        space.selectedPanel.getModelInstanceOf("ESP32").transform.idt()
            .translate(5.80f, -1.45f, 0f)
            .scale(0.5f, 0.5f, 0.5f)
                .rotate(0f, 1f, 0f, 89f)
            .rotate(0f, 0f, 1f, 60f);

        space.selectedPanel.getModelInstanceOf("3D Printer").transform.idt()
            .translate(0f, -0.5f , 0f)
            .scale(0.045f, 0.045f, 0.045f)
            .rotate(0f, 1f, 0f, 0f);

        space.selectedPanel.addObject("Blue Env");
        space.selectedPanel.addObject("ESP32");

        space.selectedPanel.addObject("3D Printer");
        space.selectedPanel.selectedObject = space.selectedPanel.getObjects().get("3D Printer");

        serverThread = new Thread(server);
        serverThread.start();


    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        space.render(deltaTime);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        space.getSceneManager().update(deltaTime);
        space.getSceneManager().render();
    }

    @Override
    public void dispose() {
        space.getSceneManager().dispose();
        space.dispose();
    }
}
