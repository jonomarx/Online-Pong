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
public class Ball {
    private double x;
    private double y;
    
    private double xSpeed = 5;
    private double ySpeed = 5;
    
    public Ball(double x, double y) {
        this.x = x;
        this.y = y;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    
    public void setY(double y) {
        this.y = y;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }

    public double getXSpeed() {
        return xSpeed;
    }
    
    public double getYSpeed() {
        return ySpeed;
    }
    
    public void setXSpeed(double xSpeed) {
        this.xSpeed = xSpeed;
    }
    
    public void setYSpeed(double ySpeed) {
        this.ySpeed = ySpeed;
    }
}
