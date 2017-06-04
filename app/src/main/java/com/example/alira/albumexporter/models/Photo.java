package com.example.alira.albumexporter.models;

import android.graphics.Bitmap;

/**
 * Created by alira on 5/30/2017.
 */

public class Photo {

    private int id;
    private Bitmap source;

    public Photo(){}

    public Photo(int id)
    {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Bitmap getSource() {
        return source;
    }

    public void setSource(Bitmap source) {
        this.source = source;
    }
}
