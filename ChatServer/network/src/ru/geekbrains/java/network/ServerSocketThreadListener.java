package ru.geekbrains.java.network;

import java.net.ServerSocket;
import java.net.Socket;

public interface ServerSocketThreadListener {

    void onServerThreadStart(ServerSocketThread thread);
    void onServerThreadStop(ServerSocketThread thread);

    void onServerSocketCreated(ServerSocketThread thread, ServerSocket serverSocket);
    void onSocketAccepted(ServerSocketThread thread, Socket socket);
    void onAcceptTimeout(ServerSocketThread thread, ServerSocket serverSocket);

    void onServerThreadException(ServerSocketThread thread, Exception e);
}
