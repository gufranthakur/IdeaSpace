package com.ideaspace.ui.panels;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class ControlPanel extends Stage {

    private IdeaSpace ideaSpace;
    private ISTable root;

    private ISButton lectureModeButton, viewModeButton, canvasModeButton, interactiveModeButton;

    public ControlPanel(IdeaSpace ideaSpace) {
        super(new ExtendViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));

        this.ideaSpace = ideaSpace;

        root = new ISTable("ui/png/ControlPanelRoot.png");
        root.setFillParent(false);
        root.pad(15);
        root.top().left();

        createUI();
    }

    private void createUI() {
        root.setWidth(2000);
        root.setHeight(120);
        root.setPosition(0, Gdx.graphics.getHeight() - 120);

        lectureModeButton = new ISButton("ui/png/lecture_mode_button.png",
            "ui/png/lecture_mode_button_hovered.png");
        root.add(lectureModeButton).width(90).height(90).padRight(10);


        viewModeButton = new ISButton(
            "ui/png/view_mode_button.png",
            "ui/png/view_mode_button_hovered.png",
            "ui/png/view_mode_button_selected.png"
        );
        canvasModeButton = new ISButton(
            "ui/png/canvas_mode_button.png",
            "ui/png/canvas_mode_button_hovered.png",
            "ui/png/canvas_mode_button_selected.png"
        );
        interactiveModeButton = new ISButton(
            "ui/png/interactive_mode_button.png",
            "ui/png/interactive_mode_button_hovered.png",
            "ui/png/interactive_mode_button_selected.png"
        );

        root.add(viewModeButton).width(80f).height(80f).padRight(5);
        root.add(canvasModeButton).width(80f).height(80f).padRight(5);
        root.add(interactiveModeButton).width(80f).height(80f).padRight(5);

        this.addActor(root);
    }

    private void deselectAllButtons(ISButton button) {
        viewModeButton.setChecked(false);
        canvasModeButton.setChecked(false);
        interactiveModeButton.setChecked(false);

        button.setChecked(true);
    }

    public void render() {
        this.act();
        this.draw();
    }
    public void resize(int width, int height) {
        this.getViewport().update(width, height, true);
    }
}
