package com.ideaspace.models;

import java.util.ArrayList;

public class Lecture {

    private String lectureName, semester, subjectName;
    public ArrayList<Slide> slides;

    public Lecture(String lectureName, String subjectName, String semester) {
        this.lectureName = lectureName;
        this.subjectName = subjectName;
        this.semester = semester;

        slides = new ArrayList<>(10);
    }

    public void loadSlides() {

    }

    public String getLectureName() {
        return lectureName;
    }

    public String getSemester() {
        return semester;
    }

    public String getSubjectName() {
        return subjectName;
    }


}
