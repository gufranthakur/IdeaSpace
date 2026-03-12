package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Decoder;
import com.ideaspace.core.Settings;
import com.ideaspace.handlers.*;
import com.ideaspace.models.Server;
import com.ideaspace.core.Space;
import com.ideaspace.simulationhand.CursorOverlay;
import com.ideaspace.ui.panels.ControlPanel;
import com.ideaspace.ui.panels.HUDPanel;
import com.ideaspace.ui.panels.MenuPanel;
import com.ideaspace.ui.screens.HomeScreen;
import com.ideaspace.ui.screens.SettingsScreen;
import com.kotcrab.vis.ui.VisUI;

public class IdeaSpace extends ApplicationAdapter {

    private LectureHandler lectureHandler;
    private CursorOverlay cursorOverlay;

    public HomeScreen homeScreen;

    public Space space;
    public ControlPanel controlPanel;
    public HUDPanel hudPanel;
    public MenuPanel menuPanel;

    private Server coreGesturesServer;
    private Server canvasServer;

    public Decoder decoder;
    public Settings settings;

    public ModelHandler modelHandler;
    public AnimationHandler animationHandler;
    public FileLoaderHandler fileLoaderHandler;
    private Thread simulationThread;

    public Thread canvasThread, coreGestureServerThread;

    private InputHandler inputHandler;
    private InputMultiplexer multiplexer;

    private final boolean DEBUG_MODE = false;
    private boolean lectureFlag = true;
    private boolean panelFlag = false;
    public boolean menuFlag = false;
    public boolean gestureLock = false;


    @Override
    public void create() {
        VisUI.load();

        lectureHandler = new LectureHandler(this);

        homeScreen = new HomeScreen(this);

        modelHandler = new ModelHandler(this);
        animationHandler = new AnimationHandler(this);
        fileLoaderHandler = new FileLoaderHandler(this);

        controlPanel = new ControlPanel(this);
        hudPanel = new HUDPanel(this);
        menuPanel = new MenuPanel(this);

        space = new Space(this);
        cursorOverlay = new CursorOverlay(65010);

        coreGesturesServer = new Server(this, "src/modular/core_gestures.py", 64000, false);
        canvasServer = new Server(this, "src/modular/canvas_main.py", 65005, true);

        decoder = new Decoder(this);
        settings = new Settings(this);

        inputHandler = new InputHandler(this);

        multiplexer = new InputMultiplexer();

        toggleLectureFlag(lectureFlag);
        Gdx.input.setInputProcessor(multiplexer);

        modelHandler.loadInitialModels();
        modelHandler.createModels();

        initThreads();
    }

    private void initThreads() {
        canvasThread = new Thread(canvasServer);
        coreGestureServerThread = new Thread(coreGesturesServer);

        coreGestureServerThread.start();
    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
        space.resize(width, height);
        homeScreen.resize(width, height);
        controlPanel.resize(width, height);
        hudPanel.resize(width, height);
        menuPanel.resize(width, height);
    }
    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        if (!lectureFlag) {
            homeScreen.render(deltaTime);
        } else {
            space.render(deltaTime);

            if (getPanelFlag() == true) controlPanel.render();
            if (menuFlag) menuPanel.render();

            cursorOverlay.update();
            cursorOverlay.render();

            hudPanel.render();
            animationHandler.update();
        }
    }

    @Override
    public void dispose() {
        //canvasServer.stopServer();
        coreGesturesServer.stopServer();

        homeScreen.dispose();
        modelHandler.dispose();
        space.getSceneManager().dispose();
        space.dispose();

        hudPanel.dispose();
        controlPanel.dispose();
        menuPanel.dispose();

        VisUI.dispose();
    }

    public void toggleLectureFlag(boolean lectureFlag) {

        this.lectureFlag = lectureFlag;

        multiplexer.getProcessors().clear();
        multiplexer.addProcessor(inputHandler);

        if (lectureFlag) {
            multiplexer.addProcessor(controlPanel.getStage());
            multiplexer.addProcessor(space.getCameraController());
            multiplexer.addProcessor(hudPanel.getStage());
            multiplexer.addProcessor(menuPanel);
        } else {
            multiplexer.addProcessor(homeScreen.getStage());
        }

    }

    public void toggleMenuFlag() {
        this.menuFlag = !menuFlag;
    }

    public void toggleGestureLock() {
        this.gestureLock = !gestureLock;
    }

    public void setPanelFlag(boolean panelFlag) {
        this.panelFlag = panelFlag;
    }

    public boolean getPanelFlag() {
        return panelFlag;
    }


    public LectureHandler getLectureHandler() {
        return lectureHandler;
    }

    public boolean getDebugMode() {
        return DEBUG_MODE;
    }
}
