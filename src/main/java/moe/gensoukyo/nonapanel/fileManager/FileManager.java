package moe.gensoukyo.nonapanel.fileManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Path;

public class FileManager {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path CONFIG_PATH;

    public static void init(MinecraftServer server) {
        LevelResource levelResource = new LevelResource("nonapanel.json");
        CONFIG_PATH = server.getWorldPath(levelResource);
        loadFromFile(CONFIG_PATH);
    }

    public static void loadFromFile(Path configPath) {
        CONFIG_PATH = configPath;

    }
}
