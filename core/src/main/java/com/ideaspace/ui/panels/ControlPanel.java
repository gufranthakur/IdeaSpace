package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
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

    // Track model cards
    private Array<ModelCard> loadedModelCards;
    private Array<ModelCard> libraryModelCards;

    public ControlPanel(IdeaSpace ideaSpace) {

        super(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            Gdx.graphics.getWidth() + 400, Gdx.graphics.getHeight() + 200));
        this.ideaSpace = ideaSpace;

        // Initialize arrays
        loadedModelCards = new Array<>();
        libraryModelCards = new Array<>();

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
        root.add(libraryModelsScrollPane);

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
        if (!libraryModelCards.contains(modelCard, true)) {
            libraryModelCards.add(modelCard);
            rebuildLibraryModelsTable();
        }
    }

    public void addModelCardToModelsPane(ModelCard modelCard) {
        if (!loadedModelCards.contains(modelCard, true)) {
            loadedModelCards.add(modelCard);
            rebuildLoadedModelsTable();
        }
    }

    public void removeModelCard(ModelCard modelCard) {
        loadedModelCards.removeValue(modelCard, true);
        rebuildLoadedModelsTable();
    }

    public void removeModelCardByName(String modelName) {
        ModelCard cardToRemove = null;

        // Search in loaded models
        for (ModelCard card : loadedModelCards) {
            if (card.getModelMesh().modelName.equals(modelName)) {
                cardToRemove = card;
                break;
            }
        }

        if (cardToRemove != null) {
            removeModelCard(cardToRemove);
        }
    }

    public void removeModelCardFromLibrary(ModelCard modelCard) {
        libraryModelCards.removeValue(modelCard, true);
        rebuildLibraryModelsTable();
    }

    private void rebuildLoadedModelsTable() {
        loadedModelsContentTable.clearChildren();
        for (ModelCard card : loadedModelCards) {
            loadedModelsContentTable.add(card).width(280).height(100).padBottom(10).row();
        }
    }

    private void rebuildLibraryModelsTable() {
        libraryModelsContentTable.clearChildren();
        for (ModelCard card : libraryModelCards) {
            libraryModelsContentTable.add(card).width(280).height(100).padBottom(10).row();
        }
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
