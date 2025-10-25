package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Server;
import com.ideaspace.core.Space;
import com.ideaspace.handlers.LectureHandler;
import com.ideaspace.ui.HomeScreen;
import com.ideaspace.utils.BackgroundUtils;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    private LectureHandler lectureHandler;

    public HomeScreen homeScreen;
    public Space space;
    private Server server;
    public Decoder decoder;

    public Thread serverThread;
    private InputMultiplexer multiplexer;

    private boolean lectureFlag;

    @Override
    public void create() {
        VisUI.load();

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

        space.selectedSlide.loadObject("Blue Env", "models/environments/blue_cube.glb");
        space.selectedSlide.loadObject("ESP32", "models/microcontrollers/esp.glb");
        space.selectedSlide.loadObject("3D Printer", "models/misc/3d_printer.glb");

        space.selectedSlide.getModelInstanceOf("Blue Env").transform.idt()
            .scale(3f, 2f, 2f)
                .translate(-53f, -55f, 0f);

        space.selectedSlide.getModelInstanceOf("ESP32").transform.idt()
            .translate(0f, 0f, 0f)
            .scale(0.5f, 0.5f, 0.5f)
                .rotate(0f, 1f, 0f, 89f)
            .rotate(0f, 0f, 1f, 60f);

        space.selectedSlide.addObject("Blue Env");

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

        BackgroundUtils.disposeCachedBackgrounds();
        VisUI.dispose();

    }

    public void setLectureFlag(boolean lectureFlag) {
        this.lectureFlag = lectureFlag;
    }

    public LectureHandler getLectureHandler() {
        return lectureHandler;
    }
}
