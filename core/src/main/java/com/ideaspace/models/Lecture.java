package com.ideaspace.models;

import com.badlogic.gdx.Gdx;
import com.ideaspace.IdeaSpace;
import net.mgsx.gltf.loaders.glb.GLBLoader;
import net.mgsx.gltf.scene3d.scene.Scene;
import net.mgsx.gltf.scene3d.scene.SceneAsset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class Lecture {

    private String lectureName;


    public Lecture(String lectureName) {
        this.lectureName = lectureName;
    }



    public String getLectureName() {
        return lectureName;
    }


}
