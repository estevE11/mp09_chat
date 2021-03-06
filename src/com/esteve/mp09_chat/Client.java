package com.esteve.mp09_chat; /**
 2  * Cliente.java
 3  *
 4  * @author Luis Alejandro Bernal
 5  *
 6  * La parte cliente de un chat de un solo canal. Su objetivo es did�ctivo por lo que trata de
 7  * ser muy sencillo.
 8  */

import org.json.simple.JSONObject;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.Socket;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

public class Client {
    public static void main(String[] args)throws IOException {
        String direccion = "localhost";
        int puerto = 9999;
        String login = "Nadie";

        if(args.length >= 1){
            login = args[0];
        }
        if(args.length >= 2){
            direccion = args[1];
        }
        if(args.length >= 3){
            puerto = Integer.parseInt(args[2]);
        }
        Socket socket = new Socket(direccion, puerto);

        Talk talk = new Talk(socket, login);
        talk.hablar();

        socket.close();
        System.exit(0);
    }

}
class AccionEnviar implements ActionListener{
    private JTextField areaTexto;
    private PrintStream salida;
    private String login;
    private int id = -1;

    public AccionEnviar(Socket s, JTextField at, String l){
        areaTexto = at;
        try {
            salida = new PrintStream(s.getOutputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        login = l;
    }

    public void actionPerformed(ActionEvent e){
        if(id < 0) return;
        salida.println(login + "> " + areaTexto.getText() );
        areaTexto.setText("");
    }

    private void setId(int id) {
        this.id = id;
    }
}

class Talk {
    private Socket socket;
    private String login;

    public Talk(Socket s, String l){
        socket = s;
        login = l;
    }

    public void hablar(){
        JFrame marco = new JFrame(login);
        marco.setLayout(new BorderLayout());
        JTextArea areaTexto = new JTextArea("");
        areaTexto.setEditable(false);
        marco.add(areaTexto, "Center");
        JPanel panel = new JPanel(new FlowLayout());
        marco.add(panel, "South");
        JTextField campoTexto = new JTextField(30);
        panel.add(campoTexto);
        JButton botonEnviar = new JButton("Enviar");
        AccionEnviar ae = new AccionEnviar(socket, campoTexto, login);
        botonEnviar.addActionListener(ae);
        panel.add(botonEnviar);
        marco.setSize(600,800);
        marco.setVisible(true);

        BufferedReader entrada;
        PrintStream salida;
        int id = 0;
        try {
            salida = new PrintStream(socket.getOutputStream());
            this.sendConnectedPacket(salida);

            entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            String packet;
            while( (packet = entrada.readLine()) != null){
                this.parsePacket(packet);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void parsePacket(String packet) {
        // Imprimir missatge rebut per pantalla
        //areaTexto.setText(areaTexto.getText() + mensaje + "\n");
        System.out.println(packet);
    }

    private void sendConnectedPacket(PrintStream salida) {
        JSONObject packet = new JSONObject();
        packet.put("type", "connect");

        JSONObject data = new JSONObject();
        data.put("username", this.login);
        packet.put("data", data);

        salida.println(packet.toJSONString());
    }

    private void sendMessagePacket(PrintStream salida, String message) {
        JSONObject packet = new JSONObject();
        packet.put("type", "msg");

        JSONObject data = new JSONObject();
        data.put("message", message);
        packet.put("data", data);

        salida.println(packet.toJSONString());
    }
}

