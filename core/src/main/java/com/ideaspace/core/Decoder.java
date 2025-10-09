package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.ideaspace.IdeaSpace;

public class Decoder {

    private IdeaSpace ideaSpace;

    // Add at the top of your class or Space class
    public float cameraMoveStep = 0.1f;   // change this to increase/decrease movement speed
    public float cameraLookStep = 0.1f;   // change this to increase/decrease look sensitivity


    public Decoder(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void decode(String command) {
        switch (command) {
            // Zoom
            case "ZOOMED IN" -> ideaSpace.space.cameraMoveZ -= cameraMoveStep;
            case "ZOOMED OUT" -> ideaSpace.space.cameraMoveZ += cameraMoveStep;

            // Camera MOVE (position)
            case "CAMERA MOVE RIGHT" -> ideaSpace.space.cameraMoveX += cameraMoveStep;
            case "CAMERA MOVE LEFT" -> ideaSpace.space.cameraMoveX -= cameraMoveStep;
            case "CAMERA MOVE TOP" -> ideaSpace.space.cameraMoveY += cameraMoveStep;
            case "CAMERA MOVE BOTTOM" -> ideaSpace.space.cameraMoveY -= cameraMoveStep;
            case "CAMERA MOVE TOP-RIGHT" -> {
                ideaSpace.space.cameraMoveX += cameraMoveStep;
                ideaSpace.space.cameraMoveY += cameraMoveStep;
            }
            case "CAMERA MOVE TOP-LEFT" -> {
                ideaSpace.space.cameraMoveX -= cameraMoveStep;
                ideaSpace.space.cameraMoveY += cameraMoveStep;
            }
            case "CAMERA MOVE BOTTOM-RIGHT" -> {
                ideaSpace.space.cameraMoveX += cameraMoveStep;
                ideaSpace.space.cameraMoveY -= cameraMoveStep;
            }
            case "CAMERA MOVE BOTTOM-LEFT" -> {
                ideaSpace.space.cameraMoveX -= cameraMoveStep;
                ideaSpace.space.cameraMoveY -= cameraMoveStep;
            }

            // Camera LOOK (direction)
            case "CAMERA LOOK RIGHT" -> ideaSpace.space.cameraLookX += cameraLookStep;
            case "CAMERA LOOK LEFT" -> ideaSpace.space.cameraLookX -= cameraLookStep;
            case "CAMERA LOOK TOP" -> ideaSpace.space.cameraLookY += cameraLookStep;
            case "CAMERA LOOK BOTTOM" -> ideaSpace.space.cameraLookY -= cameraLookStep;
            case "CAMERA LOOK TOP-RIGHT" -> {
                ideaSpace.space.cameraLookX += cameraLookStep;
                ideaSpace.space.cameraLookY += cameraLookStep;
            }
            case "CAMERA LOOK TOP-LEFT" -> {
                ideaSpace.space.cameraLookX -= cameraLookStep;
                ideaSpace.space.cameraLookY += cameraLookStep;
            }
            case "CAMERA LOOK BOTTOM-RIGHT" -> {
                ideaSpace.space.cameraLookX += cameraLookStep;
                ideaSpace.space.cameraLookY -= cameraLookStep;
            }
            case "CAMERA LOOK BOTTOM-LEFT" -> {
                ideaSpace.space.cameraLookX -= cameraLookStep;
                ideaSpace.space.cameraLookY -= cameraLookStep;
            }

            case "NULL" -> {
                ideaSpace.space.cameraMoveX = 0f;
                ideaSpace.space.cameraMoveY = 0f;
                ideaSpace.space.cameraMoveZ = 0f;
                ideaSpace.space.cameraLookX = 0f;
                ideaSpace.space.cameraLookY = 0f;
            }

            default -> System.out.println("Invalid command received: " + command);
        }




    }

}
