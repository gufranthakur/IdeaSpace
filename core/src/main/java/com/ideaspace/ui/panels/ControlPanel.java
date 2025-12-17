package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.ideaspace.ui.components.ModelCard;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

public class ControlPanel extends Stage{

    private IdeaSpace ideaSpace;

    private ISTable root;

    private ISButton loadedModelButton, libraryModelButton;

    public ControlPanel(IdeaSpace ideaSpace) {

        super(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            Gdx.graphics.getWidth() + 400, Gdx.graphics.getHeight() + 200));
        this.ideaSpace = ideaSpace;

        root = new ISTable("ui/png/ControlPanelBG.png");
        root.setFillParent(false);
        root.top().left();

        createUI();
    }

    private void createUI() {
        root.setWidth(320);
        root.setHeight(870);
        root.setPosition(0, Gdx.graphics.getHeight() - root.getHeight());
        root.padTop(50);

        loadedModelButton = new ISButton(
            "ui/png/loaded_model_button.png",
            "ui/png/loaded_model_button_hovered.png",
            "ui/png/loaded_model_button_selected.png"
        );

        libraryModelButton = new ISButton(
            "ui/png/library_model_button.png",
            "ui/png/library_model_button_hovered.png",
            "ui/png/library_model_button_selected.png"
        );

        Table table = new Table();
        table.center();
        table.padRight(5);
        table.padLeft(5);
        table.align(Align.center);
        table.add(loadedModelButton).width(120f).height(80f).fillX().expandX();
        table.add(libraryModelButton).width(120f).height(80f).fillX().expandX();

        root.add(table).center().fillX().expandX().row();

        root.add(new ModelCard("Arduino Uno Model")).width(300).height(100).padBottom(10).row();
        root.add(new ModelCard("ESP-32")).width(300).height(100).padBottom(10).row();
        root.add(new ModelCard("RaspberryPi")).width(300).height(100).padBottom(10).row();
        root.add(new ModelCard("PLC")).width(300).height(100).padBottom(10).row();

        this.addActor(root);
    }

    public void render(float deltaTime) {
        this.act();
        this.draw();
    }

    public void resize(int width, int height) {
        this.getViewport().update(width, height, true);
    }

    public Stage getStage() {
        return this;
    }


}
