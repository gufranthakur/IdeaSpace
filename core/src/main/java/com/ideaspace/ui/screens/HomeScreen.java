package com.ideaspace.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.Gdx2DPixmap;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScalingViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class HomeScreen {

    private IdeaSpace ideaSpace;

    private boolean DEBUG_MODE;

    private Stage stage;
    private VisTable root;

    private LectureScreen lectureScreen;

    private ISTable navigationTable, contentTable;

    ISButton lectureButton, modelsButton, howToUseButton, settingsButton, logOutButton;

    public HomeScreen(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        Gdx.app.debug("Crazy", "Hello world");

        DEBUG_MODE = ideaSpace.getDebugMode();
        lectureScreen = new LectureScreen(this, DEBUG_MODE);

        stage = new Stage(new ScreenViewport());
        root = new VisTable();
        root.setFillParent(true);
        root.top().left();

        navigationTable = new ISTable("ui/png/ButtonsPanel.png");
        navigationTable.top().left();

        stage.addActor(root);

        root.setDebug(DEBUG_MODE);
        navigationTable.setDebug(DEBUG_MODE);

        createUI();
    }

    public void createUI() {
        lectureButton = new ISButton("ui/png/LecturesButton.png");
        modelsButton = new ISButton("ui/png/3D-Models-Button.png");
        howToUseButton = new ISButton("ui/png/HowToUseButton.png");
        settingsButton = new ISButton("ui/png/SettingsButton.png");
        logOutButton = new ISButton("ui/png/ExitButton.png");

        navigationTable.padTop(250);

        navigationTable.add(lectureButton).fill().width(215).height(60).pad(5).row();
        navigationTable.add(modelsButton).fill().height(60).pad(5).row();
        navigationTable.add(howToUseButton).fill().height(60).pad(5).row();
        navigationTable.add(settingsButton).fill().height(60).pad(5).row();
        navigationTable.add(logOutButton).fill().height(60).pad(5).row();

        root.add(navigationTable).width(300).pad(10).padBottom(120);
        root.add(lectureScreen).expand().fill();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void render(float deltaTime) {
        stage.act(deltaTime);
        stage.draw();
    }

    public void dispose() {

        lectureButton.dispose();
        modelsButton.dispose();
        settingsButton.dispose();
        howToUseButton.dispose();
        logOutButton.dispose();

        navigationTable.dispose();

        lectureScreen.dispose();

        stage.dispose();
    }

    public Stage getStage() {
        return stage;
    }

    public IdeaSpace getIdeaSpace() {
        return this.ideaSpace;
    }




}
