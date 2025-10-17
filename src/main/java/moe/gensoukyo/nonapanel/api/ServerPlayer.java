package moe.gensoukyo.nonapanel.api;

public class ServerPlayer {
    public final String name;
    public final String uuid;
    public SimpleVec3 location;
    public String dimension;
    public int ping;
    public int permissionLevel;

    public ServerPlayer(String name, String uuid, SimpleVec3 location, String dimension, int ping, int permissionLevel) {
        this.name = name;
        this.uuid = uuid;
        this.location = location;
        this.dimension = dimension;
        this.ping = ping;
        this.permissionLevel = permissionLevel;
    }
}
