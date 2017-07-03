package com.example.alira.albumexporter.util;

// The sole purpose of this class is to systematize the behavior of
// loadMoreContent() method under AlbumsActivity and PhotosActivity classes
// this class prevent this method from making redundant calls.

public class Locker
{
    private boolean lock;

    public void lock(){lock = true;}
    public void unlock(){lock = false;}
    public boolean isLocked(){return lock;}

}