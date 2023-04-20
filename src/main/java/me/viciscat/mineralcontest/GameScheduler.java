package me.viciscat.mineralcontest;

public class GameScheduler implements Runnable{

    public GameHandler handler;

    /**
     * When an object implementing interface {@code Runnable} is used
     * to create a thread, starting the thread causes the object's
     * {@code run} method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method {@code run} is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    public GameScheduler(GameHandler gameHandler) {
        handler = gameHandler;
    }


    @Override
    public void run() {

    }
}
