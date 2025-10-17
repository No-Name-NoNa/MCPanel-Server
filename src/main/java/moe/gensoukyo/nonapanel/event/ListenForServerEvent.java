package moe.gensoukyo.nonapanel.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import moe.gensoukyo.nonapanel.api.ServerPlayer;
import moe.gensoukyo.nonapanel.api.SimpleVec3;
import moe.gensoukyo.nonapanel.config.PanelConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@EventBusSubscriber
public class ListenForServerEvent {

    private static final int LISTEN_PORT = 25570;
    private static final String ACCESS_KEY = "123456";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final List<ServerPlayer> players = new ArrayList<>();
    private static final List<ClientSession> clients = new CopyOnWriteArrayList<>();
    private static boolean serverRunning = false;
    private static ServerSocket serverSocket;
    private static ServerConfig config;
    private static PanelConfig panelConfig;
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerRunning(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= 20) {
            tickCounter = 0;
            syncPlayers(event.getServer());
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        configInitialize(server);
        panelConfig = new PanelConfig(server.getWorldData().getLevelName(), server.getMotd(), server.getServerVersion());

        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(config.port);
                serverRunning = true;
                log("Listening for client connections on port " + config.port);

                while (serverRunning) {
                    Socket client = serverSocket.accept();
                    new Thread(() -> handleClient(client)).start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "MCPanel-Listener").start();
    }

    private static void configInitialize(MinecraftServer server) {
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        File configFile = new File(worldDir, "mc_panel.json");
        loadServerConfig(configFile);
    }

    private static void handleClient(Socket client) {
        try {
            DataInputStream in = new DataInputStream(client.getInputStream());
            DataOutputStream out = new DataOutputStream(client.getOutputStream());

            String accessKey = in.readUTF();
            log("Received access key: " + accessKey);

            if (checkAccessKey(accessKey)) {
                out.writeUTF("OK");
                log("Client authenticated successfully.");

                String panelJson = GSON.toJson(panelConfig);
                out.writeUTF(panelJson);
                out.flush();

                ClientSession session = new ClientSession(client, out);
                clients.add(session);
                log("Client added: " + client.getRemoteSocketAddress());

                new Thread(() -> session.listen(in), "MCPanel-Client-" + client.getPort()).start();

            } else {
                out.writeUTF("INVALID_KEY");
                out.flush();
                client.close();
                log("Client authentication failed: " + client.getRemoteSocketAddress());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean checkAccessKey(String key) {
        return config.accessKey.equals(key);
    }

    private static void log(String message) {
        System.out.println("[MCPanel] " + message);
    }

    private static void loadServerConfig(File configFile) {
        try {
            if (configFile.exists()) {
                try (FileReader reader = new FileReader(configFile)) {
                    config = GSON.fromJson(reader, ServerConfig.class);
                    log("Loaded config: " + GSON.toJson(config));
                }
            } else {
                config = new ServerConfig();
                try (FileWriter writer = new FileWriter(configFile)) {
                    GSON.toJson(config, writer);
                    log("Created default config at: " + configFile.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            log("Failed to load/create config: " + e.getMessage());
        }
    }

    private static void syncPlayers(MinecraftServer server) {
        List<ServerPlayer> current = new ArrayList<>();

        server.getPlayerList().getPlayers().forEach(p -> {
            ServerPlayer sp = new ServerPlayer(
                    p.getGameProfile().getName(),
                    p.getUUID().toString(),
                    new SimpleVec3((float) p.getX(), (float) p.getY(), (float) p.getZ()),
                    p.level().dimension().location().toString(),
                    p.connection.latency(),
                    server.getProfilePermissions(p.getGameProfile())
            );
            current.add(sp);
        });

        List<ServerPlayer> added = new ArrayList<>();
        List<ServerPlayer> removed = new ArrayList<>();
        List<ServerPlayer> updated = new ArrayList<>();

        for (ServerPlayer now : current) {
            ServerPlayer old = findPlayerByUUID(players, now.uuid);
            if (old == null) {
                added.add(now);
            } else if (!now.equals(old)) {
                updated.add(now);
            }
        }

        for (ServerPlayer old : players) {
            if (findPlayerByUUID(current, old.uuid) == null) {
                removed.add(old);
            }
        }

        if (!added.isEmpty() || !removed.isEmpty() || !updated.isEmpty()) {
            sendPlayerDiff(added, removed, updated);
        }

        players.clear();
        players.addAll(current);
    }

    private static ServerPlayer findPlayerByUUID(List<ServerPlayer> list, String uuid) {
        for (ServerPlayer p : list) {
            if (p.uuid.equals(uuid)) return p;
        }
        return null;
    }

    private static void sendPlayerDiff(List<ServerPlayer> added, List<ServerPlayer> removed, List<ServerPlayer> updated) {
        try {
            DiffPacket diff = new DiffPacket(added, removed, updated);
            String json = GSON.toJson(diff);
            broadcastToClients(json);
            log("Sent player diff: " + json);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void broadcastToClients(String message) {
        for (ClientSession client : clients) {
            if (!client.send(message)) {
                clients.remove(client);
                log("Removed disconnected client.");
            }
        }
    }

    private record ClientSession(Socket socket, DataOutputStream out) {

        void listen(DataInputStream in) {
            try {
                while (!socket.isClosed() && socket.isConnected()) {
                    String msg = in.readUTF();
                    log("Received from client: " + msg);
                    if ("PING".equals(msg)) {
                        send("PONG");
                    }
                }
            } catch (IOException ignored) {
            } finally {
                close();
            }
        }

        boolean send(String msg) {
            try {
                out.writeUTF(msg);
                out.flush();
                return true;
            } catch (IOException e) {
                close();
                return false;
            }
        }

        void close() {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static class DiffPacket {
        List<ServerPlayer> added;
        List<ServerPlayer> removed;
        List<ServerPlayer> updated;

        DiffPacket(List<ServerPlayer> a, List<ServerPlayer> r, List<ServerPlayer> u) {
            this.added = a;
            this.removed = r;
            this.updated = u;
        }
    }

    public static class ServerConfig {
        public String accessKey = ACCESS_KEY;
        public int port = LISTEN_PORT;
        public String description = "Default MCPanel configuration";
    }
}
