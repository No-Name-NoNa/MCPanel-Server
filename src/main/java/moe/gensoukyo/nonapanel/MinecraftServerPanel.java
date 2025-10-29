package moe.gensoukyo.nonapanel;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(MinecraftServerPanel.MOD_ID)
public class MinecraftServerPanel {
    public static final String MOD_ID = "nonapanel";
    public static final Logger LOGGER = LoggerFactory.getLogger("MCPanel");

    public MinecraftServerPanel(IEventBus modEventBus, ModContainer modContainer) {
    }
}
