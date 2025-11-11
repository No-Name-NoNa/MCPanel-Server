package moe.gensoukyo.nonapanel.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import moe.gensoukyo.nonapanel.event.ServerEvent;
import moe.gensoukyo.nonapanel.handler.MessageHandler;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static moe.gensoukyo.nonapanel.handler.MessageHandler.MAX_MESSAGE;

@Getter
@Setter
public class ClientSession {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Queue<String> broadcastMessages = new ConcurrentLinkedQueue<>();
    private final Queue<ServerStatus> tickCache = new ConcurrentLinkedQueue<>();
    private final Socket socket;
    private final DataOutputStream out;
    private Status lastStatus = Status.INFO;

    public ClientSession(Socket socket, DataOutputStream out) {
        this.socket = socket;
        this.out = out;
    }

    public void listen(DataInputStream in) {
        try {
            while (!socket.isClosed() && socket.isConnected()) {
                String msg = in.readUTF();
                ServerEvent.log("Received from client: " + msg);
                handleClientMessage(msg);
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    public void handleClientMessage(String msg) {
        if (lastStatus == Status.OPTION) return;
        if (msg.contains("-")) {
            handleSpecialMessage(msg);
            return;
        }
        switch (msg) {
            case "INFO" -> sendServerInfo();
            case "MODS" -> sendModsInfo();
            case "PLAYERS" -> sendPlayersInfo();
            case "PING" -> sendPingResponse();
            case "CHAT" -> sendChatMessages();
            case "CHAT_CONTINUE" -> sendContinuedChatMessages();
            case "STATUS" -> sendStatusMessages();
            case "STATUS_CONTINUE" -> sendContinuedStatusMessages();
            default -> logUnknownMessage();
        }
    }

    private void handleSpecialMessage(String msg) {
        String[] split = msg.split("-", 2);
        String first = split[0];
        msg = split[1];

        switch (first) {
            case "COMMAND" -> handleConsoleCommand(msg);
            case "CHAT" -> handleChatMessages(msg);
            case "DETAILED_PLAYER" -> handleDetailedPlayerMessage(msg);
            default -> {
            }
        }
    }

    private void handleConsoleCommand(String msg) {
        MessageHandler.addChatMessage(msg, "[Server]");
        ServerEvent.getCommandQueue().add(msg);
    }

    private void handleDetailedPlayerMessage(String msg) {
        lastStatus = Status.DETAILED_PLAYER.setUsername(msg);
        send(gson.toJson(ServerEvent.getUser(lastStatus.getUsername())), lastStatus);
    }

    private void handleChatMessages(String msg) {
        MessageHandler.addChatMessage(msg, "[Server]");
        ServerEvent.getChatQueue().add(msg);
    }

    private void sendServerInfo() {
        send(gson.toJson(ServerEvent.getServerInfo()), Status.INFO);
    }

    private void sendModsInfo() {
        send(gson.toJson(ServerEvent.getModInfo()), Status.MODS);
    }

    private void sendPlayersInfo() {
        send(gson.toJson(ServerEvent.getSimplePlayer()), Status.PLAYERS);
    }

    private void sendPingResponse() {
        send("PONG", Status.PING);
    }

    private void sendChatMessages() {
        send(gson.toJson(MessageHandler.messages), Status.CHAT);
        lastStatus = Status.CHAT_CONTINUE;
        broadcastMessages.clear();
    }

    private void sendContinuedChatMessages() {
        send(gson.toJson(broadcastMessages), Status.CHAT_CONTINUE);
        broadcastMessages.clear();
    }

    private void sendStatusMessages() {
        send(gson.toJson(ServerEvent.getTickQueue()), Status.STATUS);
        lastStatus = Status.STATUS_CONTINUE;
        tickCache.clear();
    }

    private void sendContinuedStatusMessages() {
        send(gson.toJson(tickCache), Status.STATUS_CONTINUE);
        tickCache.clear();
    }

    private void logUnknownMessage() {
        ServerEvent.log("Unknown message");
    }


    public void send(String msg, Status status) {
        try {
            lastStatus = status;
            out.writeUTF(status.name());
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            close();
        }
    }

    void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    public void addMessage(String msg, String username) {
        broadcastMessages.add(username + ": " + msg);
        if (broadcastMessages.size() > MAX_MESSAGE) {
            broadcastMessages.poll();
        }
    }

    public void addTick(ServerStatus status) {
        tickCache.add(status);
        if (tickCache.size() > ServerEvent.TICK_CACHE) {
            tickCache.poll();
        }
    }
}
