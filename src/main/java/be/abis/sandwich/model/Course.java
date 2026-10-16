package be.abis.sandwich.model;

import jakarta.validation.constraints.NotBlank;

public class Course {

    private int id;
    @NotBlank(message = "course title is required")
    private String title;

    public Course() {
    }

    public Course(String title) {
        this.title = title;
    }

    public Course(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
