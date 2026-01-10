package com.ideaspace.ui.screens;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.*;
import com.github.tommyettinger.textra.TextraButton;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTable;

public class HomeScreen {

    private IdeaSpace ideaSpace;

    private boolean DEBUG_MODE;

    private Stage stage;
    private Table root;

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
        root = new Table();
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
        lectureButton = new ISButton(
            "ui/png/LecturesButton.png",
            "ui/png/LecturesButton_hovered.png",
            "ui/png/LecturesButton_selected.png"
        );
        modelsButton = new ISButton(
            "ui/png/3DModelsButton.png",
            "ui/png/3DModelsButton_hovered.png",
            "ui/png/3DModelsButton_selected.png"
        );
        howToUseButton = new ISButton(
            "ui/png/HowToUseButton.png",
            "ui/png/HowToUseButton_hovered.png",
            "ui/png/HowToUseButton_selected.png"
        );
        settingsButton = new ISButton(
            "ui/png/SettingsButton.png",
            "ui/png/SettingsButton_hovered.png",
            "ui/png/SettingsButton_selected.png"
        );
        logOutButton = new ISButton(
            "ui/png/ExitButton.png",
            "ui/png/ExitButton_hovered.png"
        );

        lectureButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchScreen(lectureScreen, lectureButton);
            }
        });

        modelsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchScreen(modelsScreen, modelsButton);
            }
        });

        howToUseButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Add your how-to-use screen here when ready
                deselectAllButtons();
                howToUseButton.setChecked(true);
            }
        });

        settingsButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Add your settings screen here when ready
                deselectAllButtons();
                settingsButton.setChecked(true);
            }
        });

        logOutButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                // Add your log out logic here
                // Note: logOutButton doesn't have a selected state, so no need to select it
            }
        });

        navigationTable.padTop(20);
        navigationTable.align(Align.top);

        navigationTable.add(new ISTable("ui/png/Logo.png")).width(180).height(180).row();
        navigationTable.add(lectureButton).width(180).height(60).pad(5).row();
        navigationTable.add(modelsButton).width(180).height(60).pad(5).row();
        navigationTable.add(howToUseButton).width(180).height(60).pad(5).row();
        navigationTable.add(settingsButton).width(180).height(60).pad(5).row();
        navigationTable.add(logOutButton).width(180).height(60).pad(5).row();

        root.add(navigationTable).width(220).pad(10);
        root.add(lectureScreen).expand().fill();

        // Set initial selected button
        lectureButton.setChecked(true);
    }

    private void switchScreen(Actor newScreen, ISButton selectedButton) {
        root.getCells().get(1).setActor(newScreen);
        deselectAllButtons();
        selectedButton.setChecked(true);
    }

    private void deselectAllButtons() {
        lectureButton.setChecked(false);
        modelsButton.setChecked(false);
        howToUseButton.setChecked(false);
        settingsButton.setChecked(false);
        // logOutButton doesn't have a selected state, so we don't need to deselect it
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
