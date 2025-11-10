package moe.gensoukyo.nonapanel.event;

import moe.gensoukyo.nonapanel.api.ClientSession;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER)
public class ChatHandler {
    public static final int MAX_MESSAGE = 1000;
    public static Queue<String> messages = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void listen(ServerChatEvent event) {
        String rawMessage = event.getRawText();
        String sender = event.getUsername();
        addMessage(rawMessage, sender);
    }

    public static void addMessage(String message, String sender) {
        messages.add(sender + ": " + message);
        for (ClientSession session : ServerEvent.getClients()) {
            session.addMessage(message, sender);
        }
        if (messages.size() > MAX_MESSAGE) {
            messages.poll();
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        addMessage("main.player.joined", event.getEntity().getScoreboardName());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        addMessage("main.player.left", event.getEntity().getScoreboardName());
    }

 /*   @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof Player player) {
            addMessage("main.player.death", player.getScoreboardName());
        }
    }*/
}
