/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.client;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Hashtable;

/**
 *
 * @author Jon
 */
public class KeyTable implements KeyListener {
    private Hashtable<Integer, Boolean> table = new Hashtable<>();

    @Override
    public void keyTyped(KeyEvent e) {
        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        table.put(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        table.put(e.getKeyCode(), false);
    }
    
    public boolean isKeyPressed(int key) {
        table.putIfAbsent(key, false);
        return table.get(key);
    }
}
