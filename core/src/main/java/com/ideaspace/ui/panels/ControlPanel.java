package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class ControlPanel{

    private IdeaSpace ideaSpace;

    private Stage stage;
    private ISTable root;

    private VisTextButton addSlide, removeSlide;

    public ControlPanel(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        stage = new Stage(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(),
            Gdx.graphics.getWidth() + 400, Gdx.graphics.getHeight() + 200));
        root = new ISTable("ui/png/modelsPanelBG.png");
        root.top().left();


        createUI();
    }

    private void createUI() {
        stage.addActor(root);
    }

    public void render(float deltaTime) {
        stage.act(deltaTime);
        stage.draw();
    }

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

}
