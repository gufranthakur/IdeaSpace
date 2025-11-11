package com.ideaspace.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.ideaspace.models.Lecture;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class HistoryLectureTable extends VisTable {

    private LectureScreen lectureScreen;
    private Lecture lecture;
    private VisLabel lectureNameLabel, lectureInfoLabel;
    private VisTextButton openButton, editButton, deleteButton;

    public HistoryLectureTable(LectureScreen lectureScreen, Lecture lecture, boolean DEBUG_MODE) {
        this.lectureScreen = lectureScreen;
        this.lecture = lecture;
        this.left();
        this.setDebug(DEBUG_MODE);
        createUI();
        initListeners();
    }

    private void createUI() {
        lectureNameLabel = new VisLabel(lecture.getLectureName());
        lectureInfoLabel = new VisLabel(" | " + lecture.getSubjectName() + " | " + lecture.getSemester());

        openButton = new VisTextButton("Open");
        editButton = new VisTextButton("Edit");
        deleteButton = new VisTextButton("Delete");

        VisTable rightTable = new VisTable();
        VisTable leftTable = new VisTable();

        rightTable.add(lectureNameLabel);
        rightTable.add(lectureInfoLabel);

        leftTable.add(openButton).padRight(5).width(120f);
        leftTable.add(editButton).padRight(5).width(60f);
        leftTable.add(deleteButton).padRight(5).width(60f);

        this.add(rightTable).left().expandX().pad(10);
        this.add(leftTable).right().pad(10);

    }

    private void initListeners() {
        openButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                lectureScreen.getHomeScreen().getIdeaSpace().getLectureHandler().openLecture(lecture);
            }
        });
    }

}
