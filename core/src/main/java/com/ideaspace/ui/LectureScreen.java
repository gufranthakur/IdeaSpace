package com.ideaspace.ui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.ideaspace.models.Lecture;
import com.ideaspace.utils.BackgroundUtils;
import com.ideaspace.utils.DialogUtils;
import com.kotcrab.vis.ui.widget.*;

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

        Drawable lectureBackground = BackgroundUtils.getPrimaryBackground();
        this.setBackground(lectureBackground);

        lectureTable = new VisTable();
        historyTable = new VisTable();

        lectureTable.setDebug(debug);
        historyTable.setDebug(debug);

        createUI();

    }

    private void createUI() {
        createLectureTable();
        createHistoryTable();

        this.add(lectureTable).expand().fill().height(200).padBottom(10f).row();
        this.add(historyTable).fill().height(700).row();
    }

    private void createLectureTable() {
        createSlideButton = new VisTextButton("+");
        createSlideButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                createNewLectureDialog(homeScreen.getStage());
            }
        });


        lectureTable.left().pad(10f);
        lectureTable.add(new VisLabel("Create Lecture")).width(200f);
        lectureTable.add(new VisLabel("Starter Templates")).pad(5).row();

        lectureTable.add(createSlideButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);

        Drawable tableBG = BackgroundUtils.getSecondaryBackground();
        lectureTable.setBackground(tableBG);
        historyTable.setBackground(tableBG);
    }

    private void createHistoryTable() {
        historyTable.top().right();
    }

    private void createNewLectureDialog(Stage stage) {

        VisTextField lectureNameField = new VisTextField("Default lecture name");
        VisTextField semesterField = new VisTextField("Default sem");
        VisTextField subjectField = new VisTextField("Default sub");

        VisTextButton cancelButton = new VisTextButton("Cancel");
        VisTextButton createButton = new VisTextButton("Create");

        VisDialog dialog = new VisDialog("Create new Lecture");
        dialog.getContentTable().defaults().expandX().fillX();


        dialog.pad(20).padBottom(20);

        dialog.getContentTable().add(new VisLabel("Name: ")).padRight(10).padTop(20);
        dialog.getContentTable().add(lectureNameField).padRight(10).padTop(20).colspan(3).expand().fill().row();

        dialog.getContentTable().add(new VisLabel("Subject: ")).padRight(10);
        dialog.getContentTable().add(subjectField).padRight(10);
        dialog.getContentTable().add(new VisLabel("Semester: ")).padRight(10);
        dialog.getContentTable().add(semesterField).padRight(10).row();

        dialog.getContentTable().add(cancelButton).pad(5).colspan(2);
        dialog.getContentTable().add(createButton).pad(5).colspan(2);

        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                stage.getActors().removeIndex(dialog.getZIndex());
            }
        });

        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {

                if (lectureNameField.isEmpty()) return;
                if (subjectField.isEmpty() || semesterField.isEmpty()) return;



                Lecture lecture = new Lecture(lectureNameField.getText(),
                    subjectField.getText(),
                    semesterField.getText());

                HistoryLectureTable table = new HistoryLectureTable(getLectureScreen(), lecture);

                stage.getActors().removeIndex(dialog.getZIndex());
                homeScreen.getIdeaSpace().getLectureHandler().createNewLecture(lecture);
                historyTable.add(table).expandX().fillX().row();
            }
        });

        dialog.show(stage);
    }

    public LectureScreen getLectureScreen() {
        return this;
    }

    public HomeScreen getHomeScreen() {
        return homeScreen;
    }

}
