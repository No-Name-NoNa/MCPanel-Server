package moe.gensoukyo.nonapanel.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import moe.gensoukyo.nonapanel.event.ServerEvent;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

@Getter
@Setter
public class ClientSession {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();
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
        if (msg.contains("-")) {
            String[] parts = msg.split("-", 2);
            String firstPart = parts[0];
            String secondPart = parts[1];
            if (firstPart.equals("DETAILED_PLAYER")) {
                lastStatus = Status.DETAILED_PLAYER.setUsername(secondPart);
            }
            msg = firstPart;
        }
        switch (msg) {
            case "INFO" -> send(gson.toJson(ServerEvent.getServerInfo()), Status.INFO);
            case "MODS" -> send(gson.toJson(ServerEvent.getModInfo()), Status.MODS);
            case "PLAYERS" -> send(gson.toJson(ServerEvent.getSimplePlayer()), Status.PLAYERS);
            case "DETAILED_PLAYER" -> send(gson.toJson(ServerEvent.getUser(Status.DETAILED_PLAYER.getUsername())), Status.DETAILED_PLAYER);
            case "PING" -> send("PONG", Status.PING);
            case "STATUS" -> {
            }
            default -> ServerEvent.log("Unknown message: " + msg);
        }
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
}
