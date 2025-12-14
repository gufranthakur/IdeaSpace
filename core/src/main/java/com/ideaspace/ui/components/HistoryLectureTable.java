package com.ideaspace.ui.components;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.ideaspace.models.Lecture;
import com.ideaspace.ui.screens.LectureScreen;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;

public class HistoryLectureTable extends ISTable {

    private LectureScreen lectureScreen;
    private Lecture lecture;
    private VisLabel lectureNameLabel, lectureInfoLabel;
    private ISButton openButton, deleteButton;

    public HistoryLectureTable(LectureScreen lectureScreen, Lecture lecture, boolean DEBUG_MODE) {
        super("ui/png/HistoryTable.png");
        this.lectureScreen = lectureScreen;
        this.lecture = lecture;
        this.left();
        this.setDebug(DEBUG_MODE);
        createUI();
        initListeners();
    }

    private void createUI() {
        lectureNameLabel = new VisLabel(lecture.getLectureName());
        lectureInfoLabel = new VisLabel("");

        openButton = new ISButton("ui/png/openButton.png");
        deleteButton = new ISButton("ui/png/deleteButton.png");

        VisTable rightTable = new VisTable();
        rightTable.pad(10);
        VisTable leftTable = new VisTable();
        leftTable.pad(10);

        rightTable.add(lectureNameLabel);
        rightTable.add(lectureInfoLabel);

        leftTable.add(openButton).padRight(5).width(90).height(42); //scaled down to 0.60x
        leftTable.add(deleteButton).padRight(5).width(90).height(42);

        this.add(rightTable).left().expandX();
        this.add(leftTable).right();

    }

    private void initListeners() {
        openButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                lectureScreen.getHomeScreen().getIdeaSpace().getLectureHandler().openLecture(lecture);
            }
        });
    }


    @Override
    public void dispose() {
        openButton.dispose();
        deleteButton.dispose();


    }

}
