package com.esteve.mp09_chat; /**
 * Servidor.java
 *
 * @author Luis Alejandro Bernal
 *
 * La parte servidor de un chat de un solo canal. Su objetivo es did�ctico por lo que trata de
 * ser muy sencillo. Tiene un ciclo que siempre se cloquea en el accept, cuando se desbloquea 
 * crea un Serv, que es un servidor. �ste ;ultimo lo que hece es estar leyendo del socket
 * de su respectivo cliente y cuando llega un mensaje lo envia a la Lista se sockets.
 */

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.swing.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.LinkedList;

public class Server {
    private static final int puerto = 9999;
    private static final int max = 10000;

    public static void main(String[] args) {
        ServerSocket serverSocket;
        ListaSockets listaSockets = new ListaSockets(max);
        String[] usernames = new String[max];
        Serv[] serv = new Serv[max];
        Thread[] thread = new Thread[max];
        try {
            serverSocket = new ServerSocket(puerto);
            for(int i = 0; i < max; i++){
                Socket socket = serverSocket.accept();
                listaSockets.add(socket);
                serv[i] = new Serv(socket, listaSockets, usernames, i);
                thread[i] = new Thread(serv[i]);
                thread[i].start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ListaSockets {
    private Socket[] socket; // Los otros sockets
    private PrintStream[] salida;
    private int num;

    public ListaSockets(int n) {
        socket = new Socket[n];
        salida = new PrintStream[n];
        num = 0;
    }

    public void add(Socket s){
        try {
            salida[num] = new PrintStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        socket[num++] = s;
    }

    public int length(){
        return num;
    }

    public Socket get(int n) {
        return socket[n];
    }

    public PrintStream getSalida(int n) {
        return salida[n];
    }

    public void send(int i, String msg) {
        this.getSalida(i).println(msg);
    }
}

class Serv implements Runnable{
    private Socket socket; // Socket propio.
    private ListaSockets listaSockets; // Los otros sockets
    private String[] usernames; // Los otros sockets
    private int id;

    public Serv(Socket s, ListaSockets ls, String[] usernames, int id) {
        socket = s;
        listaSockets = ls;
        this.usernames = usernames;
        this.id = id;
    }

    public void run() {
        BufferedReader entrada;
        JSONParser jsonParser = new JSONParser();

        try {
            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String packet;
            while( (packet = entrada.readLine()) != null) {
                this.parsePacket((JSONObject) jsonParser.parse(packet));
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

    /*
    *   ESTRUCTURA PACKET:
    *   - type: connect, msg, disconnect
    *   - data:
    *       - Dades depenen del tipus
    *       - En cas de connect, el nom d'usuari
    *       - En cas de msg, el missatge i el desti (all o nom d'usuari)
    *       - En cas de disconnect, la id
    * */
    private void parsePacket(JSONObject packet) {
        System.out.println(packet);
        String type = (String) packet.get("type");
        switch (type) {
            case "connect":
                JSONObject dataCon = (JSONObject) packet.get("data");
                this.handleConnect(dataCon);
                break;
            case "msg":
                JSONObject dataMsg = (JSONObject) packet.get("data");
                this.handleMessage(dataMsg);
                break;
        }
    }

    private void handleConnect(JSONObject data) {
        String username = (String) data.get("username");
        this.usernames[this.id] = username;
        JSONObject response = new JSONObject();
        response.put("type", "none");
        response.put("status", 200);
        response.put("id", this.id);
        this.listaSockets.send(this.id, response.toJSONString());

        // Tell others user connected
        JSONObject emitPacket = new JSONObject();
        emitPacket.put("type", "connected");
        emitPacket.put("id", this.id);
        this.emit(emitPacket.toJSONString());
    }

    private void handleMessage(JSONObject data) {
        // Tell others user connected
        JSONObject emitPacket = new JSONObject();
        emitPacket.put("type", "message");
        emitPacket.put("id", this.id);

        JSONObject packetData = new JSONObject();
        packetData.put("message", (String)data.get("message"));
        packetData.put("username", this.usernames[this.id]);

        emitPacket.put("data", packetData);

        this.emit(emitPacket.toJSONString());
    }

    private void emit(String packet) {
        for (int i = 0; i < this.listaSockets.length(); i++) {
            this.listaSockets.send(i, packet);
        }
    }
}