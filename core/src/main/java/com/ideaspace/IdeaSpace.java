package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Server;
import com.ideaspace.core.Space;
import com.ideaspace.handlers.LectureHandler;
import com.ideaspace.ui.screens.HomeScreen;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    private LectureHandler lectureHandler;
    //helloooooo
    //Thank you <3 

    public HomeScreen homeScreen;
    public Space space;
    private Server server;
    public Decoder decoder;

    public Thread serverThread;
    private InputMultiplexer multiplexer;

    private final boolean DEBUG_MODE = false;
    private boolean lectureFlag;


    @Override
    public void create() {
        VisUI.load(VisUI.SkinScale.X2);


        lectureHandler = new LectureHandler(this);

        homeScreen = new HomeScreen(this);

        space = new Space(this);
        server = new Server(this);
        decoder = new Decoder(this);

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(space.getCameraController());
        multiplexer.addProcessor(homeScreen.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        space.addPanel();

        space.selectedSlide.loadObject(
            "Background",
            "models/backgrounds/dark_background.glb"
        );
        space.selectedSlide.loadObject(
            "ESP32",
            "models/microcontrollers/esp32.glb"
        );
        space.selectedSlide.loadObject(
            "3D Printer",
            "models/misc/3d_printer.glb"
        );

        space.selectedSlide
            .getModelInstanceOf("Background")
            .transform.idt()
            .scale(10f, 10f, 10f);


        space.selectedSlide.addObject("Background");
        space.selectedSlide.addObject("ESP32");

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

        if (!lectureFlag) {
            homeScreen.render(deltaTime);
        } else {
            space.getSceneManager().update(deltaTime);
            space.getSceneManager().render();
        }

    }

    @Override
    public void dispose() {
        homeScreen.dispose();

        space.getSceneManager().dispose();
        space.dispose();


        VisUI.dispose();
    }

    public void setLectureFlag(boolean lectureFlag) {
        this.lectureFlag = lectureFlag;
    }

    public LectureHandler getLectureHandler() {
        return lectureHandler;
    }

    public boolean getDebugMode() {
        return DEBUG_MODE;
    }
}
