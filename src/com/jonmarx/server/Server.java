/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.jonmarx.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.UUID;
import javax.swing.Timer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jon
 */
public class Server {
    private static Hashtable<String, String> playerStates = new Hashtable(); // playerID: STATE
    private static Hashtable<String, String> playerAddrs = new Hashtable(); // playerAddr: playerID
    private static Hashtable<String, String> playerOnlineStates = new Hashtable(); // playerAddr: ONLINE/OFFLINE, if OFFLINE when checked, it is removed
    private static Hashtable<String, Game> games = new Hashtable(); // gameID: Game
    private static DatagramSocket serverSocket;
    
    public static void main(String[] args) throws IOException {
        serverSocket = new DatagramSocket(1205);
        Timer t = new Timer(16, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    tick();
                } catch (UnknownHostException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        });
        t.start();
        
        while(!serverSocket.isClosed()) {
            DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
            serverSocket.receive(packet);
            parsePacket(packet);
        }
    }
    
    public static void parsePacket(DatagramPacket packet) throws IOException {
        String msg = new String(packet.getData(), packet.getOffset(), packet.getLength()).trim();
        String[] parts = msg.split("\\|");
        // 1: player ID
        
        switch(parts[0]) {
            case "0": // log in
                playerStates.put(parts[1], "NO GAME");
                playerAddrs.put(packet.getAddress().getHostAddress() + "/" + packet.getPort(), parts[1]);
                playerOnlineStates.put(packet.getAddress().getHostAddress() + "/" + packet.getPort(), "ONLINE");
                packet.setData("2|login".getBytes());
                serverSocket.send(packet);
                break;
            case "1": // log out
                Game game;
                if(!playerStates.get(parts[1]).equals("NO GAME")) {
                    game = games.get(playerStates.get(parts[1]));
                    game.setState("PLAYERLEFT");
                }
                playerStates.remove(parts[1]);
                playerAddrs.remove(parts[1]);
                playerOnlineStates.remove(packet.getAddress().getHostAddress() + "/" + packet.getPort());
                // no need to ensure
                break;
            case "2": // reply message / ensure msg was sent, 2: replied message, 3: data, just like code 5 serverside
                switch(parts[2]) {
                    case "isAlive":
                            playerOnlineStates.put(packet.getAddress().getHostAddress() + "/" + packet.getPort(), "ONLINE");
                        break;
                }
                break;
            case "3": // try to join game, 2 = game ID, if gameID = create, then create a new game and send the ID back
                if(parts[2].equals("create")) {
                    String id = generateID();
                    games.put(id, new Game(id));
                    parts[2] = id;
                }
                game = games.get(parts[2]);
                if(game == null) break;
                if(game.getP1() == null) {
                    playerStates.put(parts[1], parts[2]);
                    game.setP1(new Player(parts[1]));
                    game.setState("1 PLAYER READY");
                    packet.setData("2|join".getBytes());
                    serverSocket.send(packet);
                } else if(game.getP2() == null){
                    playerStates.put(parts[1], parts[2]);
                    game.setP2(new Player(parts[1]));
                    game.setState("PLAYERS READY");
                    packet.setData("2|join".getBytes());
                    serverSocket.send(packet);
                } else {
                    packet.setData("2|join|game_already_started".getBytes());
                    serverSocket.send(packet);
                }
                break;
            case "4": // send msg to game
                // no need to ensure sent, its being sent 60 times a second probably
                game = games.get(playerStates.get(parts[1]));
                Player p;
                if(game == null) break;
                if(game.getP1().getId().equals(parts[1])) {
                    p = game.getP1();
                } else if(game.getP2().getId().equals(parts[1])) {
                    p = game.getP2();
                } else {
                    p = null;
                }
                
                // 2: message, 3: boolean
                
                switch(parts[2]) {
                    case "up":
                        p.setUpPressed(parts[3].equals("true"));
                        break;
                    case "down":
                        p.setDownPressed(parts[3].equals("true"));
                        break;
                }
                break;
            case "5": // ask for SOME type of data
                StringBuilder data = new StringBuilder();
                data = data.append("2|"); // 2 means replying for data
                data = data.append(parts[1]);
                switch(parts[1]) {
                    case "games":
                        for(Game g : games.values()) {
                            if(/*g.getState().equals("1 PLAYER READY")*/ true) {
                                data = data.append("|");
                                data = data.append(g.getID());
                            }
                        }
                        break;
                    case "getID":
                        data = data.append("|");
                        data = data.append(generateID());
                        break;
                    case "players":
                        for(String player : playerAddrs.values()) {
                            data = data.append("|");
                            data = data.append(player);
                        }
                        break;
                    default:
                        data = data.append("null");
                        break;
                }
                byte[] datas = data.toString().getBytes();
                packet.setData(datas, 0, datas.length);
                serverSocket.send(packet);
                break;
        }
    }
    
    public static void disconnectPlayer(String addr) throws UnknownHostException {
        String ID = playerAddrs.get(addr);
        Game game;
        if(!playerStates.get(ID).equals("NO GAME")) {
            game = games.get(playerStates.get(ID));
            game.setState("PLAYERLEFT");
        }
        playerStates.remove(ID);
        playerAddrs.remove(ID);
        playerOnlineStates.remove(addr);
    }
    
    public static void sendMessage(DatagramPacket packet) throws IOException {
        serverSocket.send(packet);
    }
    
    private static int tick = 0;
    public static synchronized void tick() throws UnknownHostException, IOException{
        for(Game game : games.values()) {
            if(!game.getState().equals("ENDED")) {
                game.update();
                Player p1 = game.getP1();
                Player p2 = game.getP2();
                
                String[] parts1 = null;
                String[] parts2 = null;
                
                for(String addrpair : playerAddrs.keySet()) {
                    if(p1 != null && playerAddrs.get(addrpair).equals(p1.getId())) {
                        parts1 = addrpair.split("/");
                    }
                    if(p2 != null && playerAddrs.get(addrpair).equals(p2.getId())) {
                        parts2 = addrpair.split("/");
                    }
                }
                
                InetAddress addr1 = null;
                InetAddress addr2 = null;
                int port1 = 0;
                int port2 = 0;
                
                if(parts1 != null) {
                    addr1 = InetAddress.getByName(parts1[0]);
                    port1 = Integer.parseInt(parts1[1]);
                }
                
                if(parts2 != null) {
                    addr2 = InetAddress.getByName(parts2[0]);
                    port2 = Integer.parseInt(parts2[1]);
                }
                
                DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                packet.setData(("4|" + game.createGameState()).getBytes());
                if(addr1 != null) {
                    packet.setAddress(addr1);
                    packet.setPort(port1);
                    serverSocket.send(packet);
                }
                if(addr2 != null) {
                    packet.setAddress(addr2);
                    packet.setPort(port2);
                    serverSocket.send(packet);
                }
                
            } else if(game.getState().equals("ENDED")) {
                java.util.Set<String> g = games.keySet();
                for(String id : g) {
                    if(games.get(id).equals(game)) {
                        games.remove(id);
                        if(game.getP1() != null) playerStates.put(game.getP1().getId(), "NO GAME");
                        if(game.getP2() != null) playerStates.put(game.getP2().getId(), "NO GAME");
                    }
                }
            }
        }
        if(tick == 60) { // every 1 second roughly
            for(String player : playerAddrs.keySet()) {
                String[] parts = player.split("/");
                InetAddress addr = InetAddress.getByName(parts[0]);
                int port = Integer.parseInt(parts[1]);
                
                if(playerOnlineStates.get(player) == null) continue;
                
                switch(playerOnlineStates.get(player)) { // if their online or not since the last time
                    case "ONLINE": // normal
                        break;
                    case "OFFLINE3":
                        playerOnlineStates.put(player, "OFFLINE2");
                        break;
                    case "OFFLINE2":
                        playerOnlineStates.put(player, "OFFLINE1");
                        break;
                    case "OFFLINE1":
                        disconnectPlayer(player);
                        player = null;
                        break;
                }
                
                if(player == null) continue;
                
                DatagramPacket packet = new DatagramPacket(new byte[1024], 0, 1024);
                packet.setAddress(addr);
                packet.setPort(port);
                packet.setData("5|isAlive".getBytes());
                
                if(playerOnlineStates.get(player).equals("ONLINE")) {
                    playerOnlineStates.put(player, "OFFLINE3"); // should respond so it switches back to online
                }
                
                sendMessage(packet);
                
                if(playerStates.get(playerAddrs.get(player)).equals("NO GAME")) {
                    packet.setData(new byte[1024], 0, 1024);
                    packet.setData("4|NOGAME".getBytes());
                    sendMessage(packet);
                }
            }
            tick = 0;
        }
        tick++;
    }

    public static synchronized String generateID() {
        return UUID.randomUUID().toString();
    }
}
