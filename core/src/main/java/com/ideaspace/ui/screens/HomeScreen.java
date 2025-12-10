package com.ideaspace.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.*;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTable;

public class HomeScreen {

    private IdeaSpace ideaSpace;

    private boolean DEBUG_MODE;

    private Stage stage;
    private VisTable root;

    private LectureScreen lectureScreen;
    private ModelsScreen modelsScreen;

    private ISTable navigationTable;

    ISButton lectureButton, modelsButton, howToUseButton, settingsButton, logOutButton;

    public HomeScreen(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
        Gdx.app.setLogLevel(Application.LOG_DEBUG);

        Gdx.app.debug("Crazy", "Hello world");

        DEBUG_MODE = ideaSpace.getDebugMode();
        lectureScreen = new LectureScreen(this, DEBUG_MODE);
        modelsScreen = new ModelsScreen(this, DEBUG_MODE);

        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            Gdx.graphics.getWidth() + 400, Gdx.graphics.getHeight() + 200));
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

        lectureButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchScreen(lectureScreen);
            }
        });

        modelsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchScreen(modelsScreen);
            }
        });

        navigationTable.padTop(20);
        navigationTable.add(new ISTable("ui/png/IdeaSpaceLogo.png")).width(180).height(180).row();
        navigationTable.add(lectureButton).fill().width(215).height(60).pad(5).row();
        navigationTable.add(modelsButton).fill().height(60).pad(5).row();
        navigationTable.add(howToUseButton).fill().height(60).pad(5).row();
        navigationTable.add(settingsButton).fill().height(60).pad(5).row();
        navigationTable.add(logOutButton).fill().height(60).pad(5).row();

        root.add(navigationTable).width(300).pad(10);
        root.add(lectureScreen).expand().fill();
    }

    private void switchScreen(Actor newScreen) {
        root.getCells().get(1).setActor(newScreen);
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
