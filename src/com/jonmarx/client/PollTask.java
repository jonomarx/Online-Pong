/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Timer;

/**
 *
 * @author Jon
 */
public abstract class PollTask {
    protected Timer timer;
    private final Object lock = new Object();
    private boolean terminated = false;
    protected String output = null;
    private final String keyword;
    
    public PollTask(int ticks, String keyword) {
        timer = new Timer(ticks, new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                timeout();
            }
        });
        timer.setRepeats(false);
        this.keyword = keyword; // keyword that ends Timer
    }
    
    protected final void terminate() { // ends it
        terminated = true;
        timer.stop();
        synchronized(lock) {
            lock.notifyAll();
        }
    }
    public String waitUntilTermination() throws InterruptedException {
        synchronized(lock) {
            lock.wait();
        }
        return output;
    }
    public abstract void timeout(); // event timeout, SHOULD trigger terminate()
    public abstract void success(String message); // success! SHOULD trigger terminate()
    
    public final void start() {
        timer.start();
        onStart();
    }
    
    public final String getKeyword() {
        return keyword;
    }
    
    public abstract void onStart(); // initializer
    
    public final boolean isTerminated() {
        return terminated;
    }
}
