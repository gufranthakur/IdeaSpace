package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.core.Space;
import com.ideaspace.ui.components.ISButton;

public class HUDPanel extends Stage {

    private IdeaSpace ideaSpace;

    private Table root;
    private ISButton homeHUDButton, settingsHUDButton, panelHUDButton, changeMapHUDButton, importModelHUDButton;


    public HUDPanel(IdeaSpace ideaSpace) {
        super(new ExtendViewport(Gdx.graphics.getWidth() - 450, Gdx.graphics.getHeight()));
        this.ideaSpace = ideaSpace;

        root = new Table();
        root.setFillParent(true);
        root.top().right();


        createUI();
        addListeners();
    }

    private void createUI() {
        homeHUDButton = new ISButton(
            "ui/png/homeHUDButton.png",
            "ui/png/homeHUDButton_hovered.png");

        settingsHUDButton = new ISButton(
            "ui/png/settingsHUDButton.png",
            "ui/png/settingsHUDButton_hovered.png");

        panelHUDButton = new ISButton(
            "ui/png/panelHUDButton.png",
            "ui/png/panelHUDButton_hovered.png");

        changeMapHUDButton = new ISButton(
            "ui/png/changeMapHUDButton.png",
            "ui/png/changeMapHUDButton_hovered.png"
        );

        importModelHUDButton = new ISButton(
            "ui/png/importHUDButton.png",
            "ui/png/importHUDButton_hovered.png"
        );


        float buttonSize = 40f;

        root.add(importModelHUDButton).width(buttonSize).height(buttonSize).pad(3);
        root.add(changeMapHUDButton).width(buttonSize).height(buttonSize).pad(3);
        root.add(panelHUDButton).width(buttonSize).height(buttonSize).pad(3);
        root.add(settingsHUDButton).width(buttonSize).height(buttonSize).pad(3);
        root.add(homeHUDButton).width(buttonSize).height(buttonSize).pad(3);
        this.addActor(root);
    }

    private void addListeners() {
        importModelHUDButton.addListener(new ClickListener() {
           @Override
           public void clicked(InputEvent event, float x, float y) {
                ideaSpace.modelHandler.loadCustomModel();
           }
        });

        panelHUDButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                ideaSpace.setPanelFlag(!ideaSpace.getPanelFlag());
            }
        });

        homeHUDButton.addListener(new ClickListener() {
           @Override
           public void clicked(InputEvent event, float x, float y) {
               ideaSpace.toggleLectureFlag(false);
           }
        });

        changeMapHUDButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                ideaSpace.modelHandler.changeMap();
            }
        });
    }

    public void render() {
        this.act();
        this.draw();
    }


    public void resize(int width, int height) {
        this.getViewport().update(width, height, false);
    }


    public Stage getStage() {
        return this;
    }

}
