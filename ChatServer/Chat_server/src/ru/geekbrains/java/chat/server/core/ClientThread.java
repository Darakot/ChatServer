package ru.geekbrains.java.chat.server.core;

import ru.geekbrains.java.chat.library.Messages;
import ru.geekbrains.java.network.SocketThread;
import ru.geekbrains.java.network.SocketThreadListener;

import java.net.Socket;

public class ClientThread extends SocketThread {

    private String nickname;
    private boolean isAuthorized;
    private boolean isReconnecting;

    public ClientThread(SocketThreadListener listener, String name, Socket socket) {
        super(listener, name, socket);
    }

    public boolean isReconnecting() {
        return isReconnecting;
    }

    public String getNickname() {
        return nickname;
    }

    public boolean isAuthorized() {
        return isAuthorized;
    }

    void authAccept(String nickname) {
        this.nickname = nickname;
        isAuthorized = true;
        sendMessage(Messages.getAuthAccept(nickname));
    }

    void authError() {
        sendMessage(Messages.getAuthDenied());
        close();
    }

    void msgFormatError(String value) {
        sendMessage(Messages.getMsgFormatError(value));
        close();
    }

    void reconnect() {
        isReconnecting = true;
        close();
    }

}
