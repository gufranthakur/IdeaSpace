package com.ideaspace.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class ISButton extends ImageButton {

    private Texture texture;

    public ISButton(String spritePath) {
        super(createStyle(spritePath));

        this.addListener(new ClickListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                getImage().setColor(0.7f, 0.7f, 0.7f, 1); // Darken on press
                return super.touchDown(event, x, y, pointer, button);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
                getImage().setColor(1, 1, 1, 1); // Back to normal
                super.touchUp(event, x, y, pointer, button);
            }

            @Override
            public void clicked(InputEvent event, float x, float y) {
                onClicked();
            }
        });
    }

    private static ImageButtonStyle createStyle(String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(new TextureRegion(texture));
        return style;
    }

    // Override this method in subclasses or use a listener
    protected void onClicked() {
        System.out.println("Button clicked!");
    }

    public void dispose() {
        if (texture != null) {
            texture.dispose();
        }
    }
}
