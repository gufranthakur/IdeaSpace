package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.ideaspace.IdeaSpace;

public class Decoder {

    private IdeaSpace ideaSpace;

    public Decoder(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void decode(String command) {
        switch (command) {
            case "ZOOMED IN" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.space.cameraMoveRemaining -= 0.5f;
                });
            }

            case "ZOOMED OUT" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.space.cameraMoveRemaining += 0.5f;

                });
            }

            default -> System.out.println("Invalid command received");

        }
    }

}
