package com.example.alira.albumexporter.models;


import android.graphics.Bitmap;



public class Album {

    private String name;
    private String id;
    private int count;
    private String cover_photo_id;
    private Bitmap cover_photo;
    private String countWrapper;


    public Album(String id)
    {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public Bitmap getCover_photo() {
        return cover_photo;
    }

    public void setCover_photo(Bitmap cover_photo) {
        this.cover_photo = cover_photo;
    }

    public String getCountWrapper() {
        return countWrapper;
    }

    public void setCountWrapper(String countWrapper) {
        this.countWrapper = countWrapper;
    }

    public String getCover_photo_id() {
        return cover_photo_id;
    }

    public void setCover_photo_id(String cover_photo_id) {
        this.cover_photo_id = cover_photo_id;
    }


    @Override
    public String toString() {
        return "Album{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", count=" + count +
                ", countWrapper='" + countWrapper + '\'' +
                '}';
    }
}
