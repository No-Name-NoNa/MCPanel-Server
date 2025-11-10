package moe.gensoukyo.nonapanel.api;

public record ServerPlayer(String name,
                           String uuid,
                           SimpleVec3 location,
                           String dimension,
                           GameMode gamemode,
                           float health,
                           float food,
                           int ping,
                           int permissionLevel) {
}
