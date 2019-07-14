package ru.geekbrains.java.chat.server.core;

import ru.geekbrains.java.chat.library.Messages;
import ru.geekbrains.java.network.ServerSocketThread;
import ru.geekbrains.java.network.ServerSocketThreadListener;
import ru.geekbrains.java.network.SocketThread;
import ru.geekbrains.java.network.SocketThreadListener;

import java.net.ServerSocket;
import java.net.Socket;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Vector;

public class ChatServer implements ServerSocketThreadListener, SocketThreadListener{

    private ServerSocketThread serverSocketThread;
    private final DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss: ");
    private final ChatServerListener listener;

    private Vector<SocketThread> clients = new Vector<>();

    public ChatServer(ChatServerListener listener) {
        this.listener = listener;
    }

    /**
     * Chat Server Actions
     */
    public void start(int port) {
        if (serverSocketThread != null && serverSocketThread.isAlive()) {
            putLog("Server is already running");
            return;
        }
        serverSocketThread = new ServerSocketThread(this, "Server", port, 2000);
        SqlClient.connect();
//        putLog("Nick = " + SqlClient.getNick("ivan_igorevich", "123456"));
    }

    public void stop() {
        if (serverSocketThread == null || !serverSocketThread.isAlive()) {
            putLog("Server is not running");
            return;
        }
        serverSocketThread.interrupt();
        SqlClient.disconnect();
    }

    private void putLog(String msg) {
        msg = dateFormat.format(System.currentTimeMillis()) + Thread.currentThread().getName() + ": " + msg;
        listener.onChatServerLog(this, msg);
    }

    private synchronized String getUsers() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            sb.append(client.getNickname()).append(Messages.DELIMITER);
        }
        return sb.toString();
    }

    /**
     * Server Socket Thread Listener methods
     */

    @Override
    public void onServerThreadStart(ServerSocketThread thread) {
        putLog("Server started");
    }

    @Override
    public void onServerThreadStop(ServerSocketThread thread) {
        for (int i = 0; i < clients.size(); i++) {
            clients.get(i).close();
        }
        putLog("Server stopped");
    }

    @Override
    public void onServerSocketCreated(ServerSocketThread thread, ServerSocket serverSocket) {
        putLog("Server socket created");
    }

    @Override
    public void onSocketAccepted(ServerSocketThread thread, Socket socket) {
        putLog("Client connected: " + socket);
        String threadName = "SocketThread: " + socket.getInetAddress() + ":" + socket.getPort();
        new ClientThread(this, threadName, socket);
    }

    @Override
    public void onAcceptTimeout(ServerSocketThread thread, ServerSocket serverSocket) {
//        putLog("Server is alive");
    }

    @Override
    public void onServerThreadException(ServerSocketThread thread, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
    }

    /**
     * Socket Thread Listener methods
     */

    @Override
    public synchronized void onSocketThreadStart(SocketThread thread, Socket socket) {
        putLog("SocketThread started");
    }

    @Override
    public synchronized void onSocketThreadStop(SocketThread thread) {
        ClientThread client = (ClientThread) thread;
        clients.remove(thread);
        if (client.isAuthorized() && !client.isReconnecting()) {
            sendToAuthorizedClients(
                    Messages.getTypeBroadcast(
                            "Server", client.getNickname() + " disconnected"));
            sendToAuthorizedClients(Messages.getUserList(getUsers()));
        }
    }

    @Override
    public synchronized void onSocketIsReady(SocketThread thread, Socket socket) {
        clients.add(thread);
    }

    private void sendToAuthorizedClients(String msg) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized()) continue;
            client.sendMessage(msg);
        }
    }

    @Override
    public synchronized void onReceiveString(SocketThread thread, Socket socket, String msg) {
        ClientThread client = (ClientThread) thread;
        if (client.isAuthorized()) {
            handleAuthorizedClients(client, msg);
        } else {
            handleNonAuthorizedClients(client, msg);
        }
    }

    private void handleAuthorizedClients(ClientThread client, String msg) {
        String[] arr = msg.split(Messages.DELIMITER);
        String msgType = arr[0];
        switch (msgType) {
            case Messages.TYPE_BROADCAST_SHORT:
                sendToAuthorizedClients(Messages.getTypeBroadcast(client.getNickname(), arr[1]));
                break;
            default:
                client.msgFormatError(msg);
        }
    }

    private void handleNonAuthorizedClients(ClientThread newClient, String msg) {
        String[] arr = msg.split(Messages.DELIMITER);
        if (arr.length != 3 || !arr[0].equals(Messages.AUTH_REQUEST)) {
            newClient.msgFormatError(msg);
            return;
        }

        /*
        Логин через SQL БД
         */
        String login = arr[1];
        String password = arr[2];
        String nickname = SqlClient.getNick(login, password);

        if (nickname == null) {
            putLog("Invalid login/password: login='" + login + "' password='" + password + "'");
            newClient.authError();
            return;
        }

        ClientThread oldClient = findClientByNickname(nickname);

        newClient.authAccept(nickname);
        if (oldClient == null) {
            sendToAuthorizedClients(Messages.getTypeBroadcast("Server", nickname + " connected"));
        } else {
            oldClient.reconnect();
            clients.remove(oldClient);
        }

        sendToAuthorizedClients(Messages.getUserList(getUsers()));
    }

    private synchronized ClientThread findClientByNickname (String nickname) {
        for (int i = 0; i < clients.size(); i++) {
            ClientThread client = (ClientThread) clients.get(i);
            if (!client.isAuthorized())
                continue;
            if (client.getNickname().equals(nickname))
                return client;
        }
        return null;
    }

    @Override
    public synchronized void onSocketThreadException(SocketThread thread, Exception e) {
        putLog("Exception: " + e.getClass().getName() + ": " + e.getMessage());
    }

}
