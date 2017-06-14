package com.example.alira.albumexporter.util;

/**
 * Created by alira on 6/14/2017.
 */

public class Locker
{
    private boolean lock;

    public void lock(){lock = true;}
    public void unlock(){lock = false;}
    public boolean isLocked(){return lock;}

}