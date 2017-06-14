package com.example.alira.albumexporter.models;

import android.support.annotation.NonNull;


public class Album implements Comparable<Album>{

    private String name;
    private String id;
    private String count;
    private String cover_photo_id;

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

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
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
                '}';
    }

    @Override
    public int compareTo(@NonNull Album o) {

        return this.getName().compareTo(o.getName());
    }
}
