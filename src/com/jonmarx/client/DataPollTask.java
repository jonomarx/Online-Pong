/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.client;

/**
 *
 * @author Jon
 */
public class DataPollTask extends PollTask {
    
    private byte attempts = 0;
    private Runnable runner;
    
    public DataPollTask(String keyword, Runnable runner) {
        super(2000, keyword);
        this.runner = runner;
    }

    @Override
    public void timeout() {
        if(attempts == 5) {
            terminate();
        }
        attempts++;
        timer.start();
        runner.run();
    }

    @Override
    public void success(String message) {
        output = message;
        terminate();
    }

    @Override
    public void onStart() {
        runner.run();
    }
}
