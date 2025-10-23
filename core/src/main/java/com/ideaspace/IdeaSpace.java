package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Server;
import com.ideaspace.core.Space;
import com.ideaspace.ui.HomeScreen;
import com.ideaspace.utils.BackgroundGenerator;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    public HomeScreen homeScreen;
    public Space space;
    private Server server;
    public Decoder decoder;
    public Thread serverThread;

    @Override
    public void create() {
        VisUI.load();

        homeScreen = new HomeScreen(this);

        space = new Space(this);
        server = new Server(this);
        decoder = new Decoder(this);

        space.addPanel();

//        space.selectedPanel.loadObject("Blue Env", "models/environments/blue_cube.glb");
//        space.selectedPanel.loadObject("ESP32", "models/microcontrollers/esp.glb");
//        space.selectedPanel.loadObject("3D Printer", "models/misc/3d_printer.glb");
//
//        space.selectedPanel.loadObject("Rpi", "models/microcontrollers/raspberry_pi.glb");
//        space.selectedPanel.loadObject("Iphone17", "models/misc/iphone17.glb");

//        space.selectedPanel.getModelInstanceOf("Blue Env").transform.idt()
//            .scale(3f, 2f, 2f)
//                .translate(-53f, -55f, 0f);
//
//        space.selectedPanel.getModelInstanceOf("ESP32").transform.idt()
//            .translate(0f, 0f, 0f)
//            .scale(0.5f, 0.5f, 0.5f)
//                .rotate(0f, 1f, 0f, 89f)
//            .rotate(0f, 0f, 1f, 60f);
//
//        space.selectedPanel.addObject("Blue Env");
//        space.selectedPanel.addObject("ESP32");

        serverThread = new Thread(server);
        serverThread.start();


    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
        homeScreen.resize(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        space.render(deltaTime);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        homeScreen.render(deltaTime);
//        space.getSceneManager().update(deltaTime);
//        space.getSceneManager().render();
    }

    @Override
    public void dispose() {
        VisUI.dispose();
        homeScreen.dispose();
        space.getSceneManager().dispose();
        space.dispose();

        BackgroundGenerator.disposeCachedBackgrounds();
    }
}
