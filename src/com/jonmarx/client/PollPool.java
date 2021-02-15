/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.client;

import java.util.LinkedList;

/**
 *
 * @author Jon
 */
public class PollPool {
    LinkedList<PollTask> tasks = new LinkedList<>();
    public PollPool() {
        
    }
    
    public void addTask(PollTask task) {
        tasks.add(task);
        task.start();
    }
    
    public void processData(String message, String keyword) {
        for(PollTask task : tasks) {
            if(task.isTerminated()) {
                tasks.remove(task);
            } else {
                if(task.getKeyword().equals(keyword)) {
                    task.success(message);
                }
            }
        }
    }
}
