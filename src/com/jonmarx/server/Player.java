/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.server;

/**
 *
 * @author Jon
 */
public class Player {
    private double y = 100;
    private boolean isUpPressed = false;
    private boolean isDownPressed = false;
    private final String id;
    
    public Player(String id) {
        this.id = id;
    }
    
    public void update() {
        
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getY() {
        return y;
    }
    
    public boolean isUpPressed() {
        return isUpPressed;
    }
    
    public boolean isDownPressed() {
        return isDownPressed;
    }
    
    public void setUpPressed(boolean b) {
        isUpPressed = b;
    }
    
    public void setDownPressed(boolean b) {
        isDownPressed = b;
    }
    
    public String getId() {
        return id;
    }
}
