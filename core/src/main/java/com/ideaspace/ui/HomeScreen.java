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
import com.ideaspace.utils.BackgroundGenerator;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

import static com.ideaspace.utils.BackgroundGenerator.PRIMARY_COLOR;

public class HomeScreen {

    private IdeaSpace ideaSpace;

    private final boolean DEBUG_MODE = false;

    private Stage stage;
    private VisTable root;

    private LectureScreen lectureScreen;

    private VisTable navigationTable, contentTable;

    VisTextButton lectureButton, modelsButton, howToUseButton, settingsButton, logOutButton;

    public HomeScreen(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        lectureScreen = new LectureScreen(this, DEBUG_MODE);

        stage = new Stage(new ScreenViewport());
        root = new VisTable();
        root.setFillParent(true);
        root.top().left();

        navigationTable = new VisTable();
        navigationTable.top().left();

        stage.addActor(root);

        root.setDebug(DEBUG_MODE);
        navigationTable.setDebug(DEBUG_MODE);

        createUI();

        Gdx.input.setInputProcessor(stage);
    }

    public void createUI() {
        Texture imgTexture = new Texture("IdeaSpace_512.png");
        Image logoImage = new Image(imgTexture);

        lectureButton = new VisTextButton("Lectures");
        modelsButton = new VisTextButton("3D Models");
        howToUseButton = new VisTextButton("How to Use");
        settingsButton = new VisTextButton("Settings");
        logOutButton = new VisTextButton("Log Out");

        navigationTable.pad(10);
        navigationTable.add(logoImage).size(200f, 200f).row();
        navigationTable.add(lectureButton).fill().height(45).pad(5).row();
        navigationTable.add(modelsButton).fill().height(45).pad(5).row();
        navigationTable.add(howToUseButton).fill().height(45).pad(5).row();
        navigationTable.add(settingsButton).fill().height(45).pad(5).row();
        navigationTable.add(logOutButton).fill().height(45).pad(5).row();

        Drawable background = BackgroundGenerator.getPrimaryBackground();
        navigationTable.setBackground(background);

        root.add(navigationTable).growY();
        root.add(lectureScreen).expand().fill();
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
