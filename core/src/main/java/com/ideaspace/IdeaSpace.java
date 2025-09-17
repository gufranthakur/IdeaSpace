package com.ideaspace;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.*;
import com.ideaspace.core.Space;

public class IdeaSpace extends ApplicationAdapter {
    private Space space;

    @Override
    public void create() {
        space = new Space(this);

        space.addPanel("MyPanel");

        space.selectedPanel.loadObject("RaspberryPi", "models/microcontrollers/raspberry_pi.glb");
        space.selectedPanel.loadObject("3D Printer", "models/misc/3d_printer.glb");
        space.selectedPanel.loadObject("Power Supply", "models/electronicalcomponents/power_supply_device.glb");

        space.selectedPanel.getModelInstanceOf("RaspberryPi").transform.idt()
            .translate(0, 0, 0f)
            .scale(0.3f, 0.3f, 0.3f);

        space.selectedPanel.getModelInstanceOf("3D Printer").transform.idt()
            .translate(3f, 0f , 0f)
            .scale(0.04f, 0.04f, 0.04f);

        space.selectedPanel.getModelInstanceOf("Power Supply").transform.idt()
            .translate(1.5f, -2f, 0f)
            .scale(8f, 8f, 8f);
    }

    @Override
    public void resize(int width, int height) {
        space.getSceneManager().updateViewport(width, height);
    }

    @Override
    public void render() {
        float deltaTime = Gdx.graphics.getDeltaTime();

        space.render(deltaTime);

        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

        space.getSceneManager().update(deltaTime);
        space.getSceneManager().render();
    }

    @Override
    public void dispose() {
        space.getSceneManager().dispose();
        space.dispose();
    }
}
