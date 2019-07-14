package ru.geekbrains.java.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

public class SocketThread extends Thread {
    private Socket socket;
    private SocketThreadListener listener;
    private DataOutputStream out;

    public SocketThread(SocketThreadListener listener, String name, Socket socket) {
        super(name);
        this.listener = listener;
        this.socket = socket;
        start();
    }

    @Override
    public void run() {
        try {
            listener.onSocketThreadStart(this, socket);
            DataInputStream in = new DataInputStream(socket.getInputStream());
            out = new DataOutputStream(socket.getOutputStream());
            listener.onSocketIsReady(this, socket);
            while (!isInterrupted()) {
                String msg = in.readUTF();
                listener.onReceiveString(this, socket, msg);
            }
        } catch (IOException e) {
            listener.onSocketThreadException(this, e);
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                listener.onSocketThreadException(this, e);
            }
            listener.onSocketThreadStop(this);
        }
    }

    public synchronized boolean sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
            return true;
        } catch (IOException e) {
            listener.onSocketThreadException(this, e);
            close();
            return false;
        }
    }

    public void close() {
        interrupt();
        try {
            socket.close();
        } catch (IOException e) {
            listener.onSocketThreadException(this, e);
        }
    }
}
