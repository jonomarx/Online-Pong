/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.client;

import com.jonmarx.server.Game;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.Timer;

/**
 *
 * @author Jon
 */
public class Client {
    private static DatagramSocket socket;
    private static volatile Game game;
    private static JFrame renderWindow = null;
    private static KeyTable keyTable = null;
    public static final int WIDTH = 750;
    public static final int HEIGHT = 500;
    private static InetAddress serverIp;
    private static String name;
    private static int PORT = 1205;
    
    static {
        try {
            String str = JOptionPane.showInputDialog("pick a server IP, use the default if you want the default", "98.247.225.134");
            serverIp = InetAddress.getByName(str);
            JOptionPane.showMessageDialog(null, "make sure to run this WITHOUT javaw (just use java), the commands rely on keyboard input");
        } catch (UnknownHostException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    private static PollPool pool = new PollPool();
    
    public static void main(String[] args) throws SocketException, UnknownHostException, IOException {
        if(args.length == 1) {
             socket = new DatagramSocket(Integer.parseInt(args[0]));
        } else {
            socket = new DatagramSocket();
        }
        Timer t = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    tick();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        t.start();
        
        final Scanner sc = new Scanner(System.in);
        Thread thread = new Thread(new Runnable() {
            public void run() {
                System.out.println("Command Line has started up.");
                try {
                    login();
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
                System.out.println("Logged In.");
                System.out.println("Type a command, type /help to list commands");
                while(sc.hasNextLine()) {
                    String[] line = sc.nextLine().trim().split(" ");
                    switch(line[0]) {
                        case "/h":
                        case "/help":
                            System.out.println("COMMANDS:\n /h or /help: open this screen \n /list <thing>: calls server and prints thing (if subcommand is help, then it prints options) \n /join <room>/create : joins a game or creates a new one, this opens a JFrame, will fail if game is already open");
                            break;
                        case "/list":
                            if(line[1].equals("help")) {
                                System.out.println("some subcommands are 'getID' (generates an ID), 'games' lists available games");
                            } else {
                                DataPollTask task = new DataPollTask(line[1], new Runnable() {
                                    @Override
                                    public void run() {
                                        DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                                        packet.setAddress(serverIp);
                                        packet.setPort(PORT);
                                        packet.setData(("5|" + line[1]).getBytes());
                                        
                                        try {
                                            socket.send(packet);
                                        } catch (IOException ex) {
                                            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                                        }
                                    }
                                });
                                pool.addTask(task);
                                try {
                                    System.out.println(task.waitUntilTermination());
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                                }
                            }
                            break;
                        case "/join":
                            if(game != null) {
                                System.out.println("You are already in a game.");
                                break;
                            }
                            
                            DataPollTask task = new DataPollTask("join", new Runnable() {
                                @Override
                                public void run() {
                                    DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                                    packet.setAddress(serverIp);
                                    packet.setPort(PORT);
                                    packet.setData(("3|" + name + "|" + line[1]).getBytes());
                                    try {
                                        socket.send(packet);
                                    } catch (IOException ex) {
                                        Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                }
                            });
                            pool.addTask(task);
                            try {
                                String returnValue = task.waitUntilTermination();
                                System.out.println(returnValue);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                            }
                            break;

                        default:
                            System.out.println("Invalid command: press /h or /help to list commands");
                            break;
                    }
                }
            }
        });
        thread.start();
        
        while(!socket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
            socket.receive(packet);
            parsePacket(packet);
        }
    }
    
    public static void login() throws IOException {
        DataPollTask task = new DataPollTask("getID", new Runnable() {
            @Override
            public void run() {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                packet.setAddress(serverIp);
                packet.setPort(PORT);
                packet.setData(("5|getID").getBytes());
                try {
                    socket.send(packet);
                } catch (IOException ex) {
                    Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        pool.addTask(task);
        try {
            name = task.waitUntilTermination();
            if(name == null) {
                System.err.println("couldn't reach server, closing.");
                System.out.println(name);
                System.exit(0);
            }
            DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
            packet.setAddress(serverIp);
            packet.setPort(PORT);
            packet.setData(("0|" + name).getBytes());
            
            socket.send(packet);
        } catch (InterruptedException ex) {
            Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static synchronized void parsePacket(DatagramPacket packet) throws IOException {
        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
        String[] parts = msg.split("\\|");
        
        switch(parts[0]) {
            case "2":
                if(parts.length == 3) {
                    pool.processData(msg.substring(parts[1].length() + parts[0].length() + 2), parts[1]);
                } else {
                    pool.processData("", parts[1]);
                }
                break;
            case "4":
                if(game == null && renderWindow != null) {
                    renderWindow.setVisible(false);
                    renderWindow.removeKeyListener(keyTable);
                    renderWindow.dispose();
                    
                    keyTable = null;
                    renderWindow = null;
                }
                game = Game.parseGameState(parts[1]);
                break;
            case "5":
                switch(parts[1]) {
                    case "isAlive":
                        packet.setData(("2|" + name + "|isAlive").getBytes());
                        socket.send(packet);
                        break;
                }
                break;
        }
    }
    
    public static void tick() throws IOException {
        if(game != null) {
            if(renderWindow == null) {
                renderWindow = new JFrame();
                renderWindow.setSize(WIDTH, HEIGHT);
                renderWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                renderWindow.setVisible(true);
                keyTable = new KeyTable();
                renderWindow.addKeyListener(keyTable);
                renderWindow.requestFocus();
            }
            render();
            if(keyTable != null) {
                DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                packet.setAddress(serverIp);
                packet.setPort(PORT);
                packet.setData(("4|" + name + "|up|" + keyTable.isKeyPressed(KeyEvent.VK_W)).getBytes());
                socket.send(packet);
                
                packet.setData(new byte[1024], 0, 1024);
                packet.setData(("4|" + name + "|down|" + keyTable.isKeyPressed(KeyEvent.VK_S)).getBytes());
                socket.send(packet);
            }
        } else {
            // maybe something...?
        }
    }
    
    public static void render() {
        BufferedImage b = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics bg = b.getGraphics();
        Color background = Color.white;
        
        bg.setColor(background);
        bg.fillRect(0, 0, WIDTH, HEIGHT);
        if(game == null) return;
        game.render(bg);
        if(!renderWindow.isVisible()) renderWindow.setVisible(true);
        
        Graphics g = renderWindow.getContentPane().getGraphics();
        if(g != null) g.drawImage(b, 0, 0, null);
    }
}
