package com.ideaspace.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.ideaspace.IdeaSpace;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class HomeScreen {

    private IdeaSpace ideaSpace;

    private final boolean debug = false;

    private Stage stage;
    private VisTable root;

    private VisTable navigationTable, contentTable;

    VisTextButton lectureButton, modelsButton, howToUseButton, settingsButton, logOutButton;

    public HomeScreen(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        stage = new Stage(new ScreenViewport());
        root = new VisTable();
        root.setFillParent(true);
        root.top().left();

        navigationTable = new VisTable();
        navigationTable.top().left();

        stage.addActor(root);

        root.setDebug(debug);
        navigationTable.setDebug(debug);

        createUI();

        Gdx.input.setInputProcessor(stage);
    }

    public void createUI() {
        Texture imgTexture = new Texture("ideaspace_logo.jpg");
        Image logoImage = new Image(imgTexture);


        lectureButton = new VisTextButton("Lectures");
        modelsButton = new VisTextButton("3D Models");
        howToUseButton = new VisTextButton("How to Use");
        settingsButton = new VisTextButton("Settings");
        logOutButton = new VisTextButton("Log Out");

        navigationTable.defaults().pad(10);
        navigationTable.add(logoImage).size(256f, 256f).row();
        navigationTable.add(lectureButton).fill().height(50).row();
        navigationTable.add(modelsButton).fill().height(50).row();
        navigationTable.add(howToUseButton).fill().height(50).row();
        navigationTable.add(settingsButton).fill().height(50).row();
        navigationTable.add(logOutButton).fill().height(50).row();

        Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pixmap.setColor(new Color(0.16f, 0.16f, 0.16f, 1f));
        pixmap.fill();
        Drawable background = new TextureRegionDrawable(new TextureRegion(new Texture(pixmap)));
        pixmap.dispose();

        navigationTable.setBackground(background);

        root.add(navigationTable).growY();
    }

    //table.add(widget)           Add widget to cell
    //     .width(200)            Set cell width
    //     .height(100)           Set cell height
    //     .pad(10)               Padding on all sides
    //     .padLeft(5)            Specific side padding
    //     .expand()              Expand to fill space
    //     .fill()                Fill the cell
    //     .center()              Center alignment
    //     .left()                Left alignment
    //     .colspan(2)            Span multiple columns
    //     .row();                Move to next row

    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    public void render(float deltaTime) {
        stage.act(deltaTime);
        stage.draw();
    }

    public void dispose() {
        stage.dispose();
    }

}
