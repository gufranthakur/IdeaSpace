package com.ideaspace.models;

import java.util.ArrayList;

public class Lecture {

    private String lectureName;
    public ArrayList<Slide> slides;

    public Lecture(String lectureName) {
        this.lectureName = lectureName;

        slides = new ArrayList<>(10);
    }

    public void loadSlides() {

    }

    public String getLectureName() {
        return lectureName;
    }


}
