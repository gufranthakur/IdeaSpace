package com.ideaspace.handlers;

import com.ideaspace.IdeaSpace;
import com.ideaspace.models.Lecture;

import java.util.ArrayList;

public class LectureHandler {

    private IdeaSpace ideaSpace;
    public ArrayList<Lecture> lectures;
    public Lecture selectedLecture;

    public LectureHandler(IdeaSpace ideaSpace) {
        this.ideaSpace = ideaSpace;

        lectures = new ArrayList<>(10);
    }

    public void createNewLecture(Lecture newLecture) {
        lectures.add(newLecture);
        System.out.println("Lecture added : " + newLecture.getLectureName());
    }

    public void openLecture(Lecture lecture) {
        ideaSpace.toggleLectureFlag(true);
        this.selectedLecture = lecture;

    }

    public void loadLectures() {
        //TODO
        //Code to load all lectures using JSON
    }

    public void saveLecture() {
        //TODO
        //Code to save lectures using file path
    }

}
