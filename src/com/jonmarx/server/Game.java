/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.server;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.util.Random;

/**
 * This is salvaged code from a different version of pong
 * @author Jon
 */
public class Game {
    private Player p1 = null;
    private Player p2 = null;
    private Ball ball = new Ball(150, 150);
    private String STATE = "NOT STARTED";
    private final String ID;
    private int p1Score = 0;
    private int p2Score = 0;
    private int respawnTicks = 0;
    private Random randomGen = new Random();
    private double timedBallSpeed = 0;
    private int rounds = 0;
    
    private double random() {
        return randomGen.nextDouble();
    }
    
    private void flipYSpeed() {
        ball.setYSpeed(-1 * ball.getYSpeed() + ((random() - 0.5) / 100));
    }
    
    private void flipXSpeed() {
        ball.setXSpeed(-1 * ball.getXSpeed() + ((random() - 0.5) / 100));
    }
    
    private void respawn() {
        ball.setXSpeed(0);
        ball.setYSpeed(0);
        ball.setX(750 / 2 + (random() * 2 - 1) / 100);
        ball.setY(250 + (random() * 2 - 1) / 100);
        
        timedBallSpeed = 0;
        respawnTicks = 312;
        rounds++;
        
        if(rounds == 5) STATE = "ENDED";
    }
    
    private void resetBallSpeed() {
        ball.setX(750 / 2 + (random() * 2 - 1) / 100);
        ball.setY(250 + (random() * 2 - 1) / 100);
        
        ball.setXSpeed(3 + (random() * 2 - 0.5) / 100);
        ball.setYSpeed(3 + (random() * 2 - 0.5) / 100);
        timedBallSpeed = 0.01;
    }
    
    public Game(String ID) {
        this.ID = ID;
        resetBallSpeed();
        respawn();
        timedBallSpeed = 0;
        respawnTicks = 312;
    }
    
    public void setP1(Player p1) {
        this.p1 = p1;
    }
    
    public void setP2(Player p2) {
        this.p2 = p2;
    }
    
    public void update() {
        if(STATE.equals("PLAYERLEFT")) STATE = "ENDED";
        if(!STATE.equals("PLAYERS READY")) return;
        
        if(timedBallSpeed != 0) { 
            timedBallSpeed = timedBallSpeed + 0.001;
        }
			
        if(respawnTicks > 0) {			
            respawnTicks = respawnTicks - 1;
            if (respawnTicks == 0) { 			
                resetBallSpeed();
            }
        }
			
        if(p1 != null && p1.isUpPressed()) {
            p1.setY(p1.getY() - 7);
	}
        if(p1 != null && p1.isDownPressed()) {
            p1.setY(p1.getY() + 7);
	}
        if(p2 != null && p2.isUpPressed()) {
            p2.setY(p2.getY() - 7);
	}
        if(p2 != null && p2.isDownPressed()) {
            p2.setY(p2.getY() + 7);
	}
			
        if(p1 != null && p1.getY() < 0) {
            p1.setY(0);
	}
        if(p1 != null && p1.getY() > 750 - 50) {
            p1.setY(750 - 50);
	}
        if(p2 != null && p2.getY() < 0) {
            p2.setY(0);
	}
        if(p2.getY() > 750 - 50) {	
             p2.setY(750 - 50);
	} 
        int xDirection = 1;
        int yDirection = 1;
			
        if(ball.getXSpeed() < 0) {
            xDirection = -1;
	}
        if(ball.getYSpeed() < 0) {
            yDirection = -1;
	} 
        ball.setX(ball.getX() + ball.getXSpeed() + (timedBallSpeed * xDirection));
        ball.setY(ball.getY() + ball.getYSpeed() + (timedBallSpeed * yDirection));
        if(ball.getY() <= 0 || ball.getY() >= 500 - 10* 6) { 
            flipYSpeed();
	}
        Rectangle2D paddleBox1 = new Rectangle.Double(50, p1.getY(), 20, 50);
        Rectangle2D paddleBox2 = new Rectangle.Double(750 - 20 * 2 - 50, p2.getY(), 20, 50);
        Rectangle2D ballBox = new Rectangle.Double(ball.getX(), ball.getY(), 10, 10);

        if(ballBox.intersects(paddleBox1) || ballBox.intersects(paddleBox2)) { 
            flipXSpeed();
	}
			
        if(ball.getX() <= 0) {
            p2Score = (int) (p2Score + Math.floor(timedBallSpeed * 100));
            respawn();
			}
        if(ball.getX() >= 750) {
            p1Score = (int) (p1Score + Math.floor(timedBallSpeed * 100));
            respawn();
        }
    }
    
    public Player getP1() {
        return p1;
    }
    
    public Player getP2() {
        return p2;
    }
    
    public void setState(String state) {
        STATE = state;
    }
    
    public String getState() {
        return STATE;
    }
    
    public String getID() {
        return ID;
    }
    
    public String createGameState() {
        return ID + "/" + STATE + "/" + (p1 != null ? p1.getY() : 0) + "/" + (p2 != null ? p2.getY() : 0) + "/" + ball.getX() + "/" + ball.getY() + "/" + p1Score + "/" + p2Score + "/" + respawnTicks + "/" + timedBallSpeed;
    }
    
    public static Game parseGameState(String state) {
        if(state.equals("NOGAME")) return null;
        String[] parts = state.split("/");
        if(!(parts.length >= 10)) {
            return null;
        }
        Game game = new Game(parts[0]);
        game.setState(parts[1]);
        Player p1 = new Player(""); // id is irrelevent when r}ering
        Player p2 = new Player("");
        
        p1.setY(Double.parseDouble(parts[2]));
        p2.setY(Double.parseDouble(parts[3]));
        
        Ball ball = new Ball(Double.parseDouble(parts[4]), Double.parseDouble(parts[5]));
        
        game.p1 = p1;
        game.p2 = p2;
        game.ball = ball;
        game.p1Score = Integer.parseInt(parts[6]);
        game.p2Score = Integer.parseInt(parts[7]);
        game.respawnTicks = Integer.parseInt(parts[8]);
        game.timedBallSpeed = Double.parseDouble(parts[9]);
        
        return game;
    }
    
    public void render(Graphics g) {
	g.setColor(Color.BLACK);
			
        int paddleX = 50;
	if(p1 != null) g.fillRect(paddleX, (int) p1.getY(), 20, 50);
	if(p2 != null) g.fillRect(750 - 20 * 2 - 50, (int) p2.getY(), 20, 50);
	g.fillOval((int) ball.getX(), (int) ball.getY(), 10, 10);
	g.drawString("" + p1Score, 150, 100);
	g.drawString("" + p2Score, 750 - 175, 100);
	g.drawString("current point amount: " + (int) Math.floor(timedBallSpeed * 100), 325, 50);
			
	if(respawnTicks > 0) {
            g.drawString("respawning...: " + respawnTicks, 325, 150);
        }
        
        g.drawString("STATE: " + STATE, 10, 10);
    }
}
