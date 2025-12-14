package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class ControlPanel extends Stage{

    private IdeaSpace ideaSpace;

    private ISTable root;

    private VisTextButton addSlide, removeSlide;

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
        root.setWidth(240);
        root.setHeight(870);
        root.setPosition(0, Gdx.graphics.getHeight() - root.getHeight()); // Position at screen top
        this.addActor(root);
    }

    public void render(float deltaTime) {
        this.act();
        this.draw();
    }

    public void resize(int width, int height) {
        this.getViewport().update(width, height, true);
    }

}
