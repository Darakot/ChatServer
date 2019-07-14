package ru.geekbrains.java.chat.server.gui;

import ru.geekbrains.java.chat.server.core.ChatServer;
import ru.geekbrains.java.chat.server.core.ChatServerListener;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ServerGUI extends JFrame implements ActionListener, Thread.UncaughtExceptionHandler, ChatServerListener {
    private static final int POS_X = 800;
    private static final int POS_Y = 200;
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private final ChatServer chatServer = new ChatServer(this);

    private final JPanel panelTop = new JPanel(new GridLayout(1, 2));
    private final JButton btnStart = new JButton("Start");
    private final JButton btnStop = new JButton("Stop");
    private final JTextArea log = new JTextArea();

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ServerGUI());
    }

    private ServerGUI() {
        Thread.setDefaultUncaughtExceptionHandler(this);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        btnStart.addActionListener(this);
        btnStop.addActionListener(this);
        setBounds(POS_X, POS_Y, WIDTH, HEIGHT);
        setResizable(false);
        setTitle("ChatServer");
        setAlwaysOnTop(true);

        log.setEditable(false);
        log.setLineWrap(true);
        JScrollPane scrollLog = new JScrollPane(log);
        panelTop.add(btnStart);
        panelTop.add(btnStop);
        add(panelTop, BorderLayout.NORTH);
        add(scrollLog, BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnStart)
            chatServer.start(8189);
        else if (src == btnStop)
            chatServer.stop();
        else
            throw new RuntimeException("Unexpected source: " + src);
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        e.printStackTrace();
        StackTraceElement[] ste = e.getStackTrace();
        String msg;
        if (ste.length == 0)
            msg = "Empty stacktrace";
        else
            msg = e.getClass().getCanonicalName() + ": " +
                    e.getMessage() + "\n" + "\t at " + ste[0];
        JOptionPane.showMessageDialog(this, msg, "Exception", JOptionPane.ERROR_MESSAGE);
        System.exit(1);
    }

    @Override
    public void onChatServerLog(ChatServer server, String msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                log.append(msg + "\n");
                log.setCaretPosition(log.getDocument().getLength());
            }
        });
    }
}
