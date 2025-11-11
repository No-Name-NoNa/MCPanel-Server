package moe.gensoukyo.nonapanel.handler;

import moe.gensoukyo.nonapanel.api.ClientSession;
import moe.gensoukyo.nonapanel.api.ServerStatus;
import moe.gensoukyo.nonapanel.event.ServerEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(value = Dist.DEDICATED_SERVER)
public class MessageHandler {
    public static final int MAX_MESSAGE = 1000;
    public static Queue<String> messages = new ConcurrentLinkedQueue<>();

    @SubscribeEvent
    public static void listen(ServerChatEvent event) {
        String rawMessage = event.getRawText();
        String sender = event.getUsername();
        addChatMessage(rawMessage, sender);
    }

    public static void addChatMessage(String message, String sender) {
        messages.add(sender + ": " + message);
        for (ClientSession session : ServerEvent.getClients()) {
            session.addMessage(message, sender);
        }
        if (messages.size() > MAX_MESSAGE) {
            messages.poll();
        }
    }

    public static void addServerStatusMessage(ServerStatus msg) {
        for (ClientSession session : ServerEvent.getClients()) {
            session.addTick(msg);
        }
    }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        addChatMessage("main.player.joined", event.getEntity().getScoreboardName());
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        addChatMessage("main.player.left", event.getEntity().getScoreboardName());
    }

 /*   @SubscribeEvent
    public static void command(CommandEvent event){

    }*/

/*    @SubscribeEvent
    public static void death(LivingDeathEvent event) {
        if(event.getEntity() instanceof Player player) {
            addMessage("main.player.death", player.getScoreboardName());
        }
    }*/
}
