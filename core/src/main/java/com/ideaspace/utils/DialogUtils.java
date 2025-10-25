package com.ideaspace.utils;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.kotcrab.vis.ui.widget.VisDialog;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextField;

public class DialogUtils {

    public static void createNewLectureDialog(Stage stage) {
        VisDialog dialog = new VisDialog("Create new Lecture");
        dialog.pad(20).padBottom(20);

        dialog.getContentTable().add(new VisLabel("Lecture Name: ")).padRight(10);
        dialog.getContentTable().add(new VisTextField("adw")).padRight(10).row();

        dialog.show(stage);
    }

}
