package moe.gensoukyo.nonapanel.event;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import moe.gensoukyo.nonapanel.api.*;
import moe.gensoukyo.nonapanel.info.ModInfo;
import moe.gensoukyo.nonapanel.info.ServerInfo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static moe.gensoukyo.nonapanel.MinecraftServerPanel.LOGGER;
import static moe.gensoukyo.nonapanel.api.GameMode.*;

@EventBusSubscriber
public class ServerEvent {

    private static final int LISTEN_PORT = 25570;
    private static final String ACCESS_KEY = "123456";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    @Getter
    private static final List<ServerPlayer> players = new ArrayList<>();
    private static final List<ClientSession> clients = new CopyOnWriteArrayList<>();
    public static int time = 20;
    @Getter
    private static SimpleServerPlayerList simplePlayer = new SimpleServerPlayerList();
    @Getter
    @SuppressWarnings("all")
    private static List<ModInfo> modInfo = new ArrayList<>();
    private static boolean serverRunning = false;
    private static ServerSocket serverSocket;
    @Getter
    private static ServerConfig config;
    @Getter
    private static ServerInfo serverInfo;
    private static int tickCounter = 0;
    private static volatile boolean isSendingData = false;

    static {
        @SuppressWarnings("all")
        Thread dataSenderThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    if (isSendingData) {
                        for (ClientSession session : clients) {
                            session.handleClientMessage(session.getLastStatus().name());
                        }
                        isSendingData = false;
                    }
                } catch (InterruptedException e) {
                    LOGGER.error(e.getMessage());
                }
            }
        });
        dataSenderThread.start();
    }

    @SubscribeEvent
    public static void onServerRunning(ServerTickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= time) {
            serverInfo = getServerInfo(event.getServer());
            SimpleServerPlayerList players = new SimpleServerPlayerList();
            List<ServerPlayer> serverPlayers = new ArrayList<>();
            for (net.minecraft.server.level.ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
                players.getPlayerList().add(new SimpleServerPlayer(player.getScoreboardName()));
                serverPlayers.add(new ServerPlayer(
                        player.getScoreboardName(),
                        player.getStringUUID(),
                        new SimpleVec3(player.position()),
                        player.level().dimension().location().toString(),
                        convert(player.gameMode.getGameModeForPlayer()),
                        player.getHealth(),
                        player.getFoodData().getFoodLevel(),
                        player.connection.latency(),
                        player.server.getProfilePermissions(player.getGameProfile())
                ));
            }
            getPlayers().clear();
            getPlayers().addAll(serverPlayers);
            simplePlayer = players;
            isSendingData = true;
            tickCounter = 0;
        }
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        MinecraftServer server = event.getServer();
        configInitialize(server);
        initializeModList();
        serverInfo = getServerInfo(server);

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
                LOGGER.error(e.getMessage());
            }
        }, "MCPanel-Listener").start();
    }

    private static void configInitialize(MinecraftServer server) {
        File worldDir = server.getWorldPath(LevelResource.ROOT).toFile();
        File configFile = new File(worldDir, "mc_panel.json");
        loadServerConfig(configFile);
    }

    private static void initializeModList() {
        ModList modList = ModList.get();
        if (modList == null) return;
        modList.getMods().forEach(mod -> {
            String modId = mod.getModId();
            String modName = mod.getDisplayName();
            String modVersion = mod.getVersion().toString();
            String modUrl = mod.getModURL().isPresent() ? mod.getModURL().get().toString() : "";        // 默认为 "N/A"
            modInfo.add(new ModInfo(modId, modName, modVersion, modUrl));
        });
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

                String panelJson = GSON.toJson(serverInfo);
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
            LOGGER.error(e.getMessage());
        }
    }

    private static boolean checkAccessKey(String key) {
        return config.accessKey.equals(key);
    }

    public static void log(String message) {
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
            LOGGER.error(e.getMessage());
            log("Failed to load/create config: " + e.getMessage());
        }
    }

    public static ServerPlayer getUser(String username){
        for (ServerPlayer player : players) {
            if (player.name().equals(username)) return player;
        }
        return null;
    }


    public static GameMode convert(GameType type){
        return switch (type){
            case SURVIVAL -> SURVIVAL;
            case CREATIVE -> CREATIVE;
            case SPECTATOR -> SPECTATOR;
            default -> ADVENTURE;
        };
    }

    protected static ServerInfo getServerInfo(MinecraftServer server) {
        return new ServerInfo(
                server.getWorldData().getLevelName(),
                server.getMotd(), server.getServerVersion(),
                server.getPlayerCount() + "/" + server.getMaxPlayers()
        );
    }

    public static class ServerConfig {
        public String accessKey = ACCESS_KEY;
        public int port = LISTEN_PORT;
        public String description = "Default MCPanel configuration";
        public int time = ServerEvent.time;
    }
}
