package com.ideaspace.ui.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.HorizontalGroup;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.ideaspace.models.Lecture;
import com.ideaspace.ui.components.HistoryLectureTable;
import com.ideaspace.ui.components.ISButton;
import com.ideaspace.ui.components.ISTable;
import com.kotcrab.vis.ui.widget.*;

import java.util.ArrayList;

public class LectureScreen extends VisTable {

    private HomeScreen homeScreen;
    private boolean DEBUG_MODE;

    private final int LECTURE_BUTTON_WIDTH = 200;
    private final int LECTURE_BUTTON_HEIGHT = 120;

    private ISTable lectureTable, historyTable;

    private ISButton createSlideButton; //285, 80
    private ArrayList<HistoryLectureTable> historyLectureTables;

    public LectureScreen(HomeScreen homeScreen, boolean DEBUG_MODE) {
        this.homeScreen = homeScreen;
        this.top().left();
        this.pad(10);
        this.DEBUG_MODE = DEBUG_MODE;

        lectureTable = new ISTable("ui/png/LecturesTable.png");
        historyTable = new ISTable("ui/png/HistoriesTable.png");

        lectureTable.setDebug(DEBUG_MODE);
        historyTable.setDebug(DEBUG_MODE);

        historyLectureTables =  new ArrayList<>();

        createUI();

    }

    private void createUI() {
        createLectureTable();
        createHistoryTable();

        this.add(lectureTable).expand().fill().height(200).padBottom(10f).row();
        this.add(historyTable).fill().height(700).row();
    }

    private void createLectureTable() {
        createSlideButton = new ISButton(
            "ui/png/CreateLectureButton.png",
            "ui/png/CreateLectureButton_hovered.png"
        );

        lectureTable.left();
        lectureTable.padLeft(20).padTop(5);

        createSlideButton.left();
        createSlideButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent changeEvent, Actor actor) {
                createNewLectureDialog(homeScreen.getStage());
            }
        });

        lectureTable.add(createSlideButton).size(LECTURE_BUTTON_WIDTH, LECTURE_BUTTON_HEIGHT);

    }

    private void createHistoryTable() {
        historyTable.top().right();
    }

    private void createNewLectureDialog(Stage stage) {

        VisTextField lectureNameField = new VisTextField(" Default lecture name");

        ISButton cancelButton = new ISButton("ui/png/CancelButton.png");
        ISButton createButton = new ISButton("ui/png/CreateButton.png");

        VisDialog dialog = new VisDialog("");
        dialog.pad(10);

        Texture background = new Texture(Gdx.files.internal("ui/png/Dialog.png"));
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(background)));


        dialog.getContentTable().add(lectureNameField).width(450f).height(50f).right().expandX().fillX().row();

        HorizontalGroup group = new HorizontalGroup();

        group.pad(5);
        group.space(5);
        group.addActor(cancelButton);
        group.addActor(createButton);

        dialog.getContentTable().add(group).colspan(3).right();

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

                Lecture lecture = new Lecture(lectureNameField.getText());

                HistoryLectureTable table = new HistoryLectureTable(getLectureScreen(), lecture, DEBUG_MODE);
                historyLectureTables.add(table);

                stage.getActors().removeIndex(dialog.getZIndex());
                homeScreen.getIdeaSpace().getLectureHandler().createNewLecture(lecture);
                historyTable.add(table).height(80).expandX().pad(10).fillX().row();
            }
        });

        dialog.show(stage);
    }

    public void dispose() {
        createSlideButton.dispose();

        for (HistoryLectureTable table : historyLectureTables) table.dispose();


        lectureTable.dispose();
        historyTable.dispose();
    }

    public LectureScreen getLectureScreen() {
        return this;
    }

    public HomeScreen getHomeScreen() {
        return homeScreen;
    }

}
