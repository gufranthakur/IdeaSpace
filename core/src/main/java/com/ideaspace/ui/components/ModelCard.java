package com.ideaspace.ui.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Align;
import com.ideaspace.IdeaSpace;
import com.ideaspace.handlers.ModelHandler;
import com.ideaspace.models.ModelMesh;
import com.kotcrab.vis.ui.widget.VisLabel;

public class ModelCard extends ISTable{

    private ModelMesh modelMesh;

    private boolean isLoaded;
    private String modelName;

    private VisLabel modelNameLabel;
    private ISButton addButton, removeButton;


    public ModelCard(ModelHandler modelHandler, ModelMesh modelMesh, boolean isLoaded) {
        super("ui/png/Model_card.png");
        this.modelMesh = modelMesh;
        this.modelName = modelMesh.modelName;
        this.isLoaded = isLoaded;
        this.align(Align.left);
        this.pad(20);

        createUI();
        setupListeners(modelHandler);
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

        if (!isLoaded) table.add(addButton).width(90f).height(70f).right();
        else table.add(removeButton).width(90f).height(70f).right();

        this.add(table).fillX().expandX();
        this.setDebug(false);
    }

    private void setupListeners(ModelHandler modelHandler) {
        addButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {

                getModelCard().modelMesh.setIsLoaded(true);
                modelHandler.loadModel(modelMesh);
            }
        });

        removeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                modelHandler.unloadModel(modelName, getModelCard());
                getModelCard().modelMesh.setIsLoaded(false);
            }
        });
    }

    private void setLoaded(boolean isLoaded) {
        this.isLoaded = isLoaded;
    }

    private boolean getLoaded() {
        return isLoaded;
    }

    public ModelCard getModelCard() {
        return this;
    }



}
