package com.ideaspace.ui.panels;

import com.ideaspace.IdeaSpace;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTable;
import com.kotcrab.vis.ui.widget.VisTextButton;

public class ControlPanel{

    private IdeaSpace ideaSpace;
    private VisTable root;

    private VisTextButton addSlide, removeSlide;

    public ControlPanel(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        root = new VisTable();

    }

}
