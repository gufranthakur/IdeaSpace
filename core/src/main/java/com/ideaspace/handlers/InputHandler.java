package com.ideaspace.handlers;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputAdapter;
import com.ideaspace.IdeaSpace;
import com.ideaspace.core.Space;

public class InputHandler extends InputAdapter {

    private IdeaSpace ideaSpace;

    public InputHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    @Override
    public boolean keyDown(int keycode) {


        switch (keycode) {
            case Input.Keys.P -> ideaSpace.setPanelFlag(!ideaSpace.getPanelFlag());

            case Input.Keys.M -> ideaSpace.toggleMenuFlag();

            case Input.Keys.L -> ideaSpace.toggleGestureLock();
        }

        return false;
    }

}
