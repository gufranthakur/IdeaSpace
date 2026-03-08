package com.ideaspace.core;

import com.ideaspace.IdeaSpace;

public class Settings {

    private IdeaSpace ideaSpace;

    public enum RotationMultiplier {
        SLOW,
        MEDIUM,
        FAST,
        VERY_FAST
    }

    public RotationMultiplier currentRotationMultiplier;

    public enum MovementMultiplier {
        SLOW,
        MEDIUM,
        FAST,
        VERY_FAST
    }

    public enum CameraSpeed {
        SLOW,
        MEDIUM,
        FAST
    }

    public MovementMultiplier currentMovementMultiplier;

    public boolean keepBackground;

    public String currentBackground;
    public boolean keepLinearInterpolation;
    public boolean keepLighting;

    public float renderDistance;
    public int targetFPSPython;

    public boolean enableZoomGesture = true;
    public boolean enableDragGesture = true;
    public boolean enableRotateGesture = true;
    public boolean enableRemoveGesture = true;
    public boolean enableSwipeGesture = false;


    public Settings(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;
    }

    public void setKeepBackground(boolean keepBackground) {
        this.keepBackground = keepBackground;
    }

    public void setCurrentBackground(String currentBackground) {
        this.currentBackground = currentBackground;
    }

    public void setKeepLinearInterpolation(boolean keepLinearInterpolation) {
        this.keepLinearInterpolation = keepLinearInterpolation;
    }

    public void setKeepLighting(boolean keepLighting) {
        this.keepLighting = keepLighting;
    }

    public void setRenderDistance(float renderDistance) {
        this.renderDistance = renderDistance;
    }

    public void setTargetFPSPython(int targetFPSPython) {
        this.targetFPSPython = targetFPSPython;
    }

    public void setEnableZoomGesture(boolean enableZoomGesture) {
        this.enableZoomGesture = enableZoomGesture;
    }

    public void setEnableDragGesture(boolean enableDragGesture) {
        this.enableDragGesture = enableDragGesture;
    }

    public void setEnableRotateGesture(boolean enableRotateGesture) {
        this.enableRotateGesture = enableRotateGesture;
    }

    public void setEnableRemoveGesture(boolean enableRemoveGesture) {
        this.enableRemoveGesture = enableRemoveGesture;
    }

    public void setEnableSwipeGesture(boolean enableSwipeGesture) {
        this.enableSwipeGesture = enableSwipeGesture;
    }

}
