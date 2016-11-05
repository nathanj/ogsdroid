package com.example.njones.myapplication;

/**
 * Created by njones on 11/5/16.
 */

class Game implements Comparable<Game> {
    int id;
    String name;
    boolean myturn;

    @Override
    public String toString() {
        return name;
    }

    public int compareTo(Game game) {
        if (myturn && !game.myturn)
            return -1;
        if (!myturn && game.myturn)
            return 1;
        return 0;
    }
}