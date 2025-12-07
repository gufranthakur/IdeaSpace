package com.ideaspace.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.ideaspace.ui.components.HistoryLectureTable;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.tabbedpane.Tab;

public class ModelsScreen extends VisTable {

    private HomeScreen homeScreen;
    private ISTable contentTable;

    public ModelsScreen(HomeScreen homeScreen, boolean DEBUG_MODE) {
        this.homeScreen = homeScreen;
        this.pad(10);
        this.setDebug(DEBUG_MODE);
        this.top().left();

        contentTable = new ISTable("ui/png/models_screen_bg.png");
        contentTable.top();
        contentTable.pad(20);
        contentTable.padTop(70);

        createModulePackCard("ui/png/Basic_IOT_Module_Pack.png");
        createModulePackCard("ui/png/Advanced_IOT_Module_Pack.png");
        createModulePackCard("ui/png/Industrial_Automation_Module_Pack.png");
        contentTable.row();
        createModulePackCard("ui/png/Laboratary_Equipment_Module_Pack.png");
        createModulePackCard("ui/png/Device_Hardware_Module_Pack.png");
        createModulePackCard("ui/png/Drone_module_Pack.png");

        contentTable.row();


        this.add(contentTable).fill().expand();
    }


    private void createModulePackCard(String imagePath) {
        ISTable table = new ISTable(imagePath);

        table.bottom().right();
        table.pad(10);

        table.add(new ISButton("ui/png/InstallButton.png")).width(91).height(31).padRight(5);
        table.add(new ISButton("ui/png/infoButton.png")).width(27).height(31);

        contentTable.add(table).width(271).height(217).pad(5); // 0.60

    }

}
