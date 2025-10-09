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

        space.selectedPanel.loadObject("RaspberryPi", "models/microcontrollers/raspberry_pi.glb");
        space.selectedPanel.loadObject("3D Printer", "models/misc/3d_printer.glb");


        space.selectedPanel.getModelInstanceOf("RaspberryPi").transform.idt()
            .translate(2.80f, -1.45f, 0f)
            .scale(0.26f, 0.26f, 0.26f)
                .rotate(0f, 1f, 0f, 42f);

        space.selectedPanel.getModelInstanceOf("3D Printer").transform.idt()
            .translate(-1f, -1.45f , 0f)
            .scale(0.045f, 0.045f, 0.045f)
            .rotate(0f, 1f, 0f, 24f);



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
