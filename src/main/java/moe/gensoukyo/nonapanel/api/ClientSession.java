package moe.gensoukyo.nonapanel.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;
import moe.gensoukyo.nonapanel.event.ListenForServerEvent;

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
                ListenForServerEvent.log("Received from client: " + msg);
                handleClientMessage(msg);
            }
        } catch (IOException ignored) {
        } finally {
            close();
        }
    }

    public void handleClientMessage(String msg) {
        switch (msg) {
            case "INFO" -> send(gson.toJson(ListenForServerEvent.getServerInfo()), Status.INFO);
            case "MODS" -> send(gson.toJson(ListenForServerEvent.getModInfo()), Status.MODS);
            case "PLAYERS" -> send(gson.toJson(ListenForServerEvent.getSimplePlayer()), Status.PLAYERS);
            case "PING" -> send("PONG", Status.PING);
            case "STATUS" -> {
            }
            default -> ListenForServerEvent.log("Unknown message: " + msg);
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
