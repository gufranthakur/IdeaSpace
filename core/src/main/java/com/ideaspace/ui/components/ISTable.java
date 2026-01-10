package com.ideaspace.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Array;
import com.kotcrab.vis.ui.widget.VisTable;

public class ISTable extends Table {

    private Texture backgroundTexture;

    public ISTable(String backgroundPath) {
        super();

        backgroundTexture = new Texture(Gdx.files.internal(backgroundPath));
        this.setBackground(new TextureRegionDrawable(new TextureRegion(backgroundTexture)));
    }


    public void dispose() {
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }

    }
}
