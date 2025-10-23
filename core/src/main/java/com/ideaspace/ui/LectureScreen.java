package com.ideaspace.ui;

import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.ideaspace.utils.BackgroundGenerator;
import com.kotcrab.vis.ui.widget.VisImageButton;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class LectureScreen extends VisTable {

    private HomeScreen homeScreen;

    private final int LECTURE_BUTTON_WIDTH = 200;
    private final int LECTURE_BUTTON_HEIGHT = 120;

    private VisTable lectureTable, historyTable;

    private VisTextButton createSlideButton;
    private VisImageButton iotTemplateButton, tdpTemplateButton, electronicsTemplateButton;

    public LectureScreen(HomeScreen homeScreen, boolean debug) {
        this.homeScreen = homeScreen;
        this.top().left();
        this.pad(10);

        Drawable lectureBackground = BackgroundGenerator.getPrimaryBackground();
        this.setBackground(lectureBackground);

        lectureTable = new VisTable();
        historyTable = new VisTable();

        lectureTable.setDebug(debug);
        historyTable.setDebug(debug);

        createUI();
    }

    private void createUI() {
        createSlideButton = new VisTextButton("+");

        lectureTable.left().pad(10f);
        lectureTable.add(new VisLabel("Create Lecture")).width(200f);
        lectureTable.add(new VisLabel("Starter Templates")).pad(5).row();

        lectureTable.add(createSlideButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);
        lectureTable.add(iotTemplateButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);
        lectureTable.add(electronicsTemplateButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);
        lectureTable.add(tdpTemplateButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);

        Drawable tableBG = BackgroundGenerator.getSecondaryBackground();
        lectureTable.setBackground(tableBG);
        historyTable.setBackground(tableBG);


        this.add(lectureTable).expand().fill().height(200).padBottom(10f).row();
        this.add(historyTable).fill().height(700).row();
    }

}
