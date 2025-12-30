package com.ideaspace.core;

import com.badlogic.gdx.Gdx;
import com.ideaspace.IdeaSpace;

public class Decoder {

    private IdeaSpace ideaSpace;

    // Velocity-based movement (units per second)
    public float cameraLookSpeed = 1.5f;   // Look rotation speed
    public float zoomSpeed = 6.0f;         // Zoom speed

    // Smoothing factors
    private float moveSmoothing = 0.15f;   // Higher = more responsive, lower = smoother
    private float lookSmoothing = 0.12f;

    // Current target velocities
    private float targetMoveX = 0f;
    private float targetMoveY = 0f;
    private float targetMoveZ = 0f;
    private float targetLookX = 0f;
    private float targetLookY = 0f;

    // Current smoothed velocities
    private float currentMoveX = 0f;
    private float currentMoveY = 0f;
    private float currentMoveZ = 0f;
    private float currentLookX = 0f;
    private float currentLookY = 0f;

    int i = 0;

    public Decoder(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void decode(String command) {
        System.out.println("Decoder received: " + command);
        // Reset targets
        targetMoveX = 0f;
        targetMoveY = 0f;
        targetMoveZ = 0f;
        targetLookX = 0f;
        targetLookY = 0f;

        // Handle CANVAS commands
        if (command.startsWith("CANVAS ")) {
            System.out.println("Handling canvas command");
            handleCanvasCommand(command);
            return;
        }

        switch (command) {
            // Zoom
            case "ZOOM IN" -> targetMoveZ = zoomSpeed;
            case "ZOOM OUT" -> targetMoveZ = -zoomSpeed;

            // Camera LOOK (direction)
            case "ROTATE RIGHT" -> targetLookX = cameraLookSpeed;
            case "ROTATE LEFT" -> targetLookX = -cameraLookSpeed;
            case "ROTATE TOP" -> targetLookY = cameraLookSpeed;
            case "ROTATE BOTTOM" -> targetLookY = -cameraLookSpeed;

            case "ROTATE TOP-RIGHT" -> {
                targetLookX = cameraLookSpeed * 0.707f;
                targetLookY = cameraLookSpeed * 0.707f;
            }
            case "ROTATE TOP-LEFT" -> {
                targetLookX = -cameraLookSpeed * 0.707f;
                targetLookY = cameraLookSpeed * 0.707f;
            }
            case "ROTATE BOTTOM-RIGHT" -> {
                targetLookX = cameraLookSpeed * 0.707f;
                targetLookY = -cameraLookSpeed * 0.707f;
            }
            case "ROTATE BOTTOM-LEFT" -> {
                targetLookX = -cameraLookSpeed * 0.707f;
                targetLookY = -cameraLookSpeed * 0.707f;
            }

            case "SWIPED LEFT" -> {
                System.out.println("Swiped left");
            }

            case "SWIPED RIGHT" -> {
                System.out.println("Swiped right i guess");
            }

            case "DRAG" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.modelHandler.loadRandomModel();
                });

            }


            case "REMOVE" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.modelHandler.unloadModel(null, null);
                });
            }

            case "SPLIT" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.modelHandler.splitModel();
                });
            }

            case "NULL" -> {
                // Let smoothing naturally decay to zero
            }

            default -> {
                if (!command.equals("NULL")) {
                    System.out.println("Invalid command received: " + command);
                }
            }
        }
    }

    private void handleCanvasCommand(String command) {
        String[] parts = command.split(" ");

        if (parts.length < 2) {
            System.out.println("Invalid canvas command: " + command);
            return;
        }

        String action = parts[1]; // DRAW, ERASE, CLEAR, END
        System.out.println("Canvas action: " + action);

        switch (action) {
            case "DRAW", "ERASE" -> {
                if (parts.length >= 7) {
                    try {
                        float normX = Float.parseFloat(parts[2]);
                        float normY = Float.parseFloat(parts[3]);
                        int r = Integer.parseInt(parts[4]);
                        int g = Integer.parseInt(parts[5]);
                        int b = Integer.parseInt(parts[6]);
                        int thickness = parts.length >= 8 ? Integer.parseInt(parts[7]) : 5;

                        boolean isEraser = action.equals("ERASE");

                        Gdx.app.postRunnable(() -> {
                            ideaSpace.space.canvasRenderer.addPoint(normX, normY, r, g, b, thickness, isEraser);
                        });
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid canvas coordinates: " + command);
                        e.printStackTrace();
                    }
                }
            }
            case "END" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.space.canvasRenderer.endStroke();
                });
            }
            case "CLEAR" -> {
                Gdx.app.postRunnable(() -> {
                    ideaSpace.space.canvasRenderer.clearCanvas();
                });
            }
            default -> System.out.println("Unknown canvas action: " + action);
        }
    }

    public void update(float delta) {
        // Smooth interpolation to target velocities
        currentMoveX = lerp(currentMoveX, targetMoveX, moveSmoothing);
        currentMoveY = lerp(currentMoveY, targetMoveY, moveSmoothing);
        currentMoveZ = lerp(currentMoveZ, targetMoveZ, moveSmoothing);
        currentLookX = lerp(currentLookX, targetLookX, lookSmoothing);
        currentLookY = lerp(currentLookY, targetLookY, lookSmoothing);

        // Apply position movement using camera's direction vector
        if (Math.abs(currentMoveZ) > 0.001f) {
            // Move along camera's direction (forward/backward)
            ideaSpace.space.camera.position.add(
                ideaSpace.space.camera.direction.cpy().scl(currentMoveZ * delta)
            );
        }

        if (Math.abs(currentMoveX) > 0.001f) {
            // Move along camera's right vector (left/right strafe)
            ideaSpace.space.camera.position.add(
                ideaSpace.space.camera.direction.cpy()
                    .crs(ideaSpace.space.camera.up).nor()
                    .scl(currentMoveX * delta)
            );
        }

        if (Math.abs(currentMoveY) > 0.001f) {
            // Move along camera's up vector (up/down)
            ideaSpace.space.camera.position.add(
                ideaSpace.space.camera.up.cpy().scl(currentMoveY * delta)
            );
        }

        if (Math.abs(currentLookX) > 0.001f || Math.abs(currentLookY) > 0.001f) {
            // Rotate camera direction
            ideaSpace.space.camera.direction.rotate(ideaSpace.space.camera.up, -currentLookX * delta * 50f);

            // Calculate right vector for pitch rotation
            float angle = currentLookY * delta * 50f;
            ideaSpace.space.camera.direction.rotate(
                ideaSpace.space.camera.direction.cpy().crs(ideaSpace.space.camera.up).nor(),
                angle
            );

            ideaSpace.space.camera.direction.nor();
        }

        ideaSpace.space.camera.update();
    }

    private float lerp(float start, float end, float alpha) {
        return start + (end - start) * alpha;
    }
}
