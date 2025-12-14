package com.ideaspace.ui.components;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

import java.util.ArrayList;
import java.util.List;

public class ISButton extends ImageButton {

    private List<Texture> textures = new ArrayList<>();

    public ISButton(String spritePath) {
        super(createStyle(spritePath));
        setupClickListener();
    }

    public ISButton(String normalPath, String hoverPath) {
        super(createStyleWithHover(normalPath, hoverPath));
        setupClickListener();
    }

    public ISButton(String normalPath, String hoverPath, String selectedPath) {
        super(createStyleWithHoverAndSelected(normalPath, hoverPath, selectedPath));
        setupClickListener();
    }

    private static ImageButtonStyle createStyle(String spritePath) {
        Texture texture = new Texture(Gdx.files.internal(spritePath));
        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(new TextureRegion(texture));
        return style;
    }

    private static ImageButtonStyle createStyleWithHover(String normalPath, String hoverPath) {
        Texture normalTexture = new Texture(Gdx.files.internal(normalPath));
        Texture hoverTexture = new Texture(Gdx.files.internal(hoverPath));

        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(new TextureRegion(normalTexture));
        style.imageOver = new TextureRegionDrawable(new TextureRegion(hoverTexture));

        return style;
    }

    private static ImageButtonStyle createStyleWithHoverAndSelected(String normalPath, String hoverPath, String selectedPath) {
        Texture normalTexture = new Texture(Gdx.files.internal(normalPath));
        Texture hoverTexture = new Texture(Gdx.files.internal(hoverPath));
        Texture selectedTexture = new Texture(Gdx.files.internal(selectedPath));

        ImageButtonStyle style = new ImageButtonStyle();
        style.imageUp = new TextureRegionDrawable(new TextureRegion(normalTexture));
        style.imageOver = new TextureRegionDrawable(new TextureRegion(hoverTexture));
        style.imageChecked = new TextureRegionDrawable(new TextureRegion(selectedTexture));

        return style;
    }

    private void setupClickListener() {
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

    // Override this method in subclasses or use a listener
    protected void onClicked() {
        System.out.println("Button clicked!");
    }

    public void dispose() {
        for (Texture texture : textures) {
            if (texture != null) {
                texture.dispose();
            }
        }
    }
}
