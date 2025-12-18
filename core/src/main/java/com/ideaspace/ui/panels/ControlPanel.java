package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISScrollPane;
import com.ideaspace.ui.components.ISTable;
import com.ideaspace.ui.components.ModelCard;
import com.kotcrab.vis.ui.widget.VisTable;

public class ControlPanel extends Stage{

    private IdeaSpace ideaSpace;

    private ISTable root;
    private ISScrollPane loadedModelsScrollPane, libraryModelsScrollPane;
    private VisTable loadedModelsContentTable, libraryModelsContentTable;

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
            "ui/png/loaded_model_button_hovered.png"
        );



        libraryModelButton = new ISButton(
            "ui/png/library_model_button.png",
            "ui/png/library_model_button_hovered.png"
        );

        Table buttonTable = new Table();
        buttonTable.center();
        buttonTable.padRight(5);
        buttonTable.padLeft(5);
        buttonTable.align(Align.center);
        buttonTable.add(loadedModelButton).width(120f).height(80f).fillX().expandX();
        buttonTable.add(libraryModelButton).width(120f).height(80f).fillX().expandX();

        loadedModelsScrollPane = new ISScrollPane();
        libraryModelsScrollPane = new ISScrollPane();

        loadedModelsContentTable = new VisTable();
        libraryModelsContentTable = new VisTable();

        loadedModelsScrollPane.setActor(loadedModelsContentTable);
        libraryModelsScrollPane.setActor(libraryModelsContentTable);

        root.add(buttonTable).center().fillX().expandX().row();
        root.add(loadedModelsScrollPane);

        loadedModelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchPanes(loadedModelsScrollPane);
            }
        });

        libraryModelButton.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent event, float x, float y) {
                switchPanes(libraryModelsScrollPane);
            }
        });

        this.addActor(root);
    }

    private void switchPanes(ISScrollPane scrollPane) {
        root.getCells().get(1).setActor(scrollPane);
    }

    public void addModelCardToLibrary(ModelCard modelCard) {
        loadedModelsContentTable.add(modelCard).width(280).height(100).padBottom(10).row();
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
