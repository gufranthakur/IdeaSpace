package com.ideaspace.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class BackgroundGenerator {

    public static final Color PRIMARY_COLOR = new Color(0.08f, 0.08f, 0.08f, 1);
    public static final Color SECONDARY_COLOR = new Color(0.10f, 0.10f, 0.10f, 1);

    private static Drawable primaryDrawable;
    private static Drawable secondaryDrawable;

    static {
        primaryDrawable = createDrawable(PRIMARY_COLOR);
        secondaryDrawable = createDrawable(SECONDARY_COLOR);
    }

    private static Drawable createDrawable(Color color) {
        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture texture = new Texture(pixmap);
        pixmap.dispose();
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    public static Drawable getPrimaryBackground() {
        return primaryDrawable;
    }

    public static Drawable getSecondaryBackground() {
        return secondaryDrawable;
    }

    public static void disposeCachedBackgrounds() {
        if (primaryDrawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) primaryDrawable).getRegion().getTexture().dispose();
        }
        if (secondaryDrawable instanceof TextureRegionDrawable) {
            ((TextureRegionDrawable) secondaryDrawable).getRegion().getTexture().dispose();
        }
        primaryDrawable = null;
        secondaryDrawable = null;
    }
}
