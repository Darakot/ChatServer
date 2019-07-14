package ru.geekbrains.java.chat.server.core;

public interface ChatServerListener {

    void onChatServerLog(ChatServer server, String msg);

}
