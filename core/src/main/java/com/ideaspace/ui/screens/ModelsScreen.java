package com.ideaspace.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTable;

public class ModelsScreen extends VisTable {

    private HomeScreen homeScreen;

    private ScrollPane scrollPane;
    private ISTable contentTable;

    public ModelsScreen(HomeScreen homeScreen, boolean DEBUG_MODE) {
        this.homeScreen = homeScreen;
        this.setDebug(DEBUG_MODE);
        this.top().left();

        contentTable = new ISTable("ui/png/scrollpane.png");
        scrollPane = new ScrollPane(contentTable);





        this.add(scrollPane);
    }


    private void createModulePackCard(String imagePath) {
        Texture texture = new Texture(Gdx.files.internal(imagePath));
        Image image = new Image(texture);

        contentTable.add(image);

    }

}
