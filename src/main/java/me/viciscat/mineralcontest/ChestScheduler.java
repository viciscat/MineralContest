package me.viciscat.mineralcontest;

import me.viciscat.mineralcontest.game.GameHandler;

public class ChestScheduler implements Runnable{

    public GameHandler handler;


    public ChestScheduler(GameHandler gameHandler) {
        handler = gameHandler;
    }


    @Override
    public void run() {

    }
}
