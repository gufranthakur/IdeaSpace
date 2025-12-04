package com.ideaspace.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisTable;

public class ISTable extends VisTable {

    private Texture backgroundTexture;
    private Array<Texture> childTextures;

    public ISTable(String backgroundPath) {
        super();
        childTextures = new Array<>();

        backgroundTexture = new Texture(Gdx.files.internal(backgroundPath));
        this.setBackground(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));
    }

    public ISTable(String backgroundPath, boolean setVisDefaults) {
        super(setVisDefaults);
        childTextures = new Array<>();

        backgroundTexture = new Texture(Gdx.files.internal(backgroundPath));
        this.setBackground(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));
    }

    // Constructor without background for transparent table
    public ISTable() {
        super();
        childTextures = new Array<>();
    }

    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
        for (Texture texture : childTextures) {
            texture.dispose();
        }
        childTextures.clear();
    }
}
