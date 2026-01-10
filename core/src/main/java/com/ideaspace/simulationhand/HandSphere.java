package com.ideaspace.simulationhand;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

import net.mgsx.gltf.scene3d.attributes.PBRColorAttribute;

public class HandSphere {

    private ModelInstance instance;
    private Vector3 position;

    public HandSphere(Model sphereModel, Color color) {
        instance = new ModelInstance(sphereModel);
        // Use PBR attributes instead of basic ColorAttribute
        instance.materials.get(0).set(PBRColorAttribute.createBaseColorFactor(color));
        instance.materials.get(0).set(PBRColorAttribute.createEmissive(new Color(color.r * 0.1f, color.g * 0.1f, color.b * 0.1f, 1f)));
        position = new Vector3();
    }

    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
        instance.transform.setToTranslation(position);
    }

    public void updateModel(Model newModel, Color color) {
        instance = new ModelInstance(newModel);
        instance.materials.get(0).set(PBRColorAttribute.createBaseColorFactor(color));
        instance.materials.get(0).set(PBRColorAttribute.createEmissive(new Color(color.r * 0.1f, color.g * 0.1f, color.b * 0.1f, 1f)));
        instance.transform.setToTranslation(position);
    }

    public ModelInstance getInstance() {
        return instance;
    }

    public Vector3 getPosition() {
        return position;
    }
}
