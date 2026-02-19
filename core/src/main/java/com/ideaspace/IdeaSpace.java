package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.models.Server;
import com.ideaspace.core.Space;
import com.ideaspace.handlers.AnimationHandler;
import com.ideaspace.handlers.LectureHandler;
import com.ideaspace.handlers.ModelHandler;
import com.ideaspace.simulationhand.ScriptExecutor;
import com.ideaspace.ui.panels.ControlPanel;
import com.ideaspace.ui.panels.ModelControlPanel;
import com.ideaspace.ui.screens.HomeScreen;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    private LectureHandler lectureHandler;

    public HomeScreen homeScreen;
    public Space space;

    public ControlPanel controlPanel;
    public ModelControlPanel modelControlPanel;

    private Server coreGesturesServer;
    private Server canvasServer;

    public Decoder decoder;

    public ModelHandler modelHandler;
    public AnimationHandler animationHandler;

    private ScriptExecutor scriptExecutor;
    private Thread simulationThread;

    public Thread canvasThread, coreGestureServerThread;

    private InputMultiplexer multiplexer;

    private final boolean DEBUG_MODE = false;
    private boolean lectureFlag = true;


    @Override
    public void create() {
        VisUI.load();

        lectureHandler = new LectureHandler(this);

        homeScreen = new HomeScreen(this);

        modelControlPanel = new ModelControlPanel(this);
        controlPanel = new ControlPanel(this);

        space = new Space(this);

        coreGesturesServer = new Server(this, "src/modular/core_gestures.py", 65000, true);
        canvasServer = new Server(this, "src/modular/canvas_main.py", 65005, true);

        scriptExecutor = new ScriptExecutor(this);

        decoder = new Decoder(this);

        modelHandler = new ModelHandler(this);
        animationHandler = new AnimationHandler(this);

        multiplexer = new InputMultiplexer();
        toggleLectureFlag(lectureFlag);
        Gdx.input.setInputProcessor(multiplexer);

        modelHandler.loadInitialModels();
        modelHandler.createModels();

        initThreads();
    }

    private void initThreads() {
        //--------these ones are not always active so no memory issues I hope so---------
        canvasThread = new Thread(canvasServer);

        //-------------------------------------------------------------------------------
        coreGestureServerThread = new Thread(coreGesturesServer);

        simulationThread = new Thread(scriptExecutor);
        simulationThread.start();

       // canvasThread.start();
        coreGestureServerThread.start();


    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
        space.resize(width, height);
        homeScreen.resize(width, height);
        modelControlPanel.resize(width, height);
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
            modelControlPanel.render();
            //controlPanel.render();
            animationHandler.update();
        }
    }

    @Override
    public void dispose() {
        //canvasServer.stopServer();
        coreGesturesServer.stopServer();

        scriptExecutor.stopPythonScript();

        homeScreen.dispose();
        modelHandler.dispose();
        space.getSceneManager().dispose();
        space.dispose();

        VisUI.dispose();
    }

    public void toggleLectureFlag(boolean lectureFlag) {

        multiplexer.getProcessors().clear();

        if (lectureFlag) {
            multiplexer.addProcessor(modelControlPanel.getStage());
            multiplexer.addProcessor(space.getCameraController());

            //multiplexer.addProcessor(controlPanel);
        } else {
            multiplexer.addProcessor(homeScreen.getStage());
        }

    }

    public void setLectureFlag(boolean lectureFlag) {
        this.lectureFlag = lectureFlag;
        toggleLectureFlag(lectureFlag);
    }

    public LectureHandler getLectureHandler() {
        return lectureHandler;
    }

    public boolean getDebugMode() {
        return DEBUG_MODE;
    }
}
