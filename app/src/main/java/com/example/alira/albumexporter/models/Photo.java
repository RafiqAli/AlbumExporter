package com.example.alira.albumexporter.models;

public class Photo {

    private String id;

    public Photo(String id)
    {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }


    @Override
    public String toString() {
        return "Photo{" +
                "id='" + id + '\'' +
                '}';
    }
}
