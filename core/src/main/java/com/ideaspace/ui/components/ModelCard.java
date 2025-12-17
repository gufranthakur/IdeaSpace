package com.ideaspace.ui.components;

import com.badlogic.gdx.maps.tiled.renderers.OrthoCachedTiledMapRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.ideaspace.ui.screens.ModelsScreen;
import com.kotcrab.vis.ui.widget.VisLabel;

public class ModelCard extends ISTable{

    private boolean isLoaded;
    private String modelName;

    private VisLabel modelNameLabel;
    private ISButton addButton, removeButton;


    public ModelCard(String modelName) {
        super("ui/png/Model_card.png");
        this.modelName = modelName;
        this.align(Align.left);
        this.pad(20);

        createUI();
        setupListeners();
    }

    private void createUI() {
        modelNameLabel = new VisLabel(modelName);

        addButton = new ISButton(
            "ui/png/addButton.png",
            "ui/png/addButton_hovered.png"
        );

        removeButton = new ISButton("ui/png/removeButton.png");

        this.add(modelNameLabel).left().padTop(5).row();

        Table table = new Table();

        table.align(Align.right);
        table.add(addButton).width(90f).height(70f).right();

        //group.addActor(removeButton);

        this.add(table).fillX().expandX();
        this.setDebug(false);
    }

    private void setupListeners() {
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (!getLoaded()) {
                    setLoaded(true);
                }
            }
        });
    }

    private void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }

    private boolean getLoaded() {
        return isLoaded;
    }

}
