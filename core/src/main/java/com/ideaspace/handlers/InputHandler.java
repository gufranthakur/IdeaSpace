package com.ideaspace.handlers;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.ideaspace.IdeaSpace;

public class InputHandler extends InputAdapter {

    private IdeaSpace ideaSpace;

    public InputHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    @Override
    public boolean keyDown(int keycode) {

        if (keycode == Input.Keys.P) {
            ideaSpace.setPanelFlag(!ideaSpace.getPanelFlag());
            System.out.println(ideaSpace.getPanelFlag());
        } else if (keycode == Input.Keys.M) {
            ideaSpace.modelHandler.changeMap();
        }

        return false;
    }

}
