package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Server;
import com.ideaspace.core.Space;
import com.ideaspace.handlers.AnimationHandler;
import com.ideaspace.handlers.LectureHandler;
import com.ideaspace.handlers.ModelHandler;
import com.ideaspace.ui.panels.ControlPanel;
import com.ideaspace.ui.screens.HomeScreen;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    private LectureHandler lectureHandler;

    public HomeScreen homeScreen;
    public Space space;
    public ControlPanel controlPanel;

    private Server rightHandServer;
    private Server leftHandServer;
    private Server zoomServer;
    private Server canvasServer;

    public Decoder decoder;

    public ModelHandler modelHandler;
    public AnimationHandler animationHandler;

    public Thread canvasThread;
    public Thread rightHandServerThread;

    private InputMultiplexer multiplexer;

    private final boolean DEBUG_MODE = false;
    private boolean lectureFlag = true;


    @Override
    public void create() {
        VisUI.load();

        lectureHandler = new LectureHandler(this);

        homeScreen = new HomeScreen(this);
        controlPanel = new ControlPanel(this);

        space = new Space(this);

        canvasServer = new Server(this, "src/modular/canvas_main.py", 65005, false);
        rightHandServer = new Server(this, "src/modular/right_main.py", 65001, false);

        decoder = new Decoder(this);

        modelHandler = new ModelHandler(this);
        animationHandler = new AnimationHandler(this);

        multiplexer = new InputMultiplexer();
        multiplexer.addProcessor(space.getCameraController());
        multiplexer.addProcessor(homeScreen.getStage());
        multiplexer.addProcessor(controlPanel.getStage());
        Gdx.input.setInputProcessor(multiplexer);

        modelHandler.loadInitialModels();
        modelHandler.createModels();

        canvasThread = new Thread(canvasServer);
        rightHandServerThread = new Thread(rightHandServer);
        rightHandServerThread.start();
        //canvasThread.start();
    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
        space.resize(width, height);
        homeScreen.resize(width, height);
        controlPanel.resize(width, height);
    }
    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (!lectureFlag) {
            homeScreen.render(deltaTime);
        } else {
            space.render(deltaTime);
            controlPanel.render(deltaTime);
            animationHandler.update();
        }
    }

    @Override
    public void dispose() {
        //canvasServer.stopServer();
        rightHandServer.stopServer();

        homeScreen.dispose();
        modelHandler.dispose();
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
