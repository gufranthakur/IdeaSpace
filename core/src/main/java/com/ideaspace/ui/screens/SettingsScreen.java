package com.ideaspace.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.ideaspace.IdeaSpace;
import com.ideaspace.ui.components.ISScrollPane;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

public class SettingsScreen extends Table {

    private HomeScreen homeScreen;
    private ISScrollPane scrollPane;
    private Table contentPane;
    public SettingsScreen(HomeScreen homeScreen) {
        this.homeScreen = homeScreen;

        //this.setFillParent(true);
        this.top().left();
        this.pad(20);

        createUI();
    }

    public void createUI() {
        scrollPane = new ISScrollPane();
        contentPane = new Table();

        scrollPane.setActor(contentPane);

        //contentPane.add(new Image((Drawable) Gdx.files.internal("ui/png/settings/graphics_text.png")));

        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();
        contentPane.add(new VisTextButton("Test")).height(150f).row();


        this.add(scrollPane);
    }

}
