package com.exam.locator;

import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import jakarta.websocket.server.ServerEndpoint;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@ServerEndpoint("/progressTrack")
public class ProgressEndpoint {

    private static final Set<Session> clients = Collections.synchronizedSet(new HashSet<Session>());

    @OnOpen
    public void onOpen(Session session) {
        System.out.println("WebSocket channel opened: Session ID " + session.getId());
        clients.add(session);
    }

    @OnClose
    public void onClose(Session session) {
        System.out.println("WebSocket channel closed: Session ID " + session.getId());
        clients.remove(session);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        System.out.println("Heartbeat from client " + session.getId() + ": " + message);
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.err.println("WebSocket exception caught: " + throwable.getMessage());
    }

    public static void sendProgressUpdate(String statusMessage) {
        synchronized (clients) {
            for (Session client : clients) {
                if (client.isOpen()) {
                    try {
                        client.getBasicRemote().sendText(statusMessage);
                    } catch (IOException e) {
                        System.err.println("Failed to push streaming data: " + e.getMessage());
                    }
                }
            }
        }
    }
}
