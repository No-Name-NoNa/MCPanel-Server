package moe.gensoukyo.nonapanel.api;


import lombok.Getter;

public enum Status {
    INFO,
    MODS,
    PLAYERS,
    DETAILED_PLAYER("dev"),
    STATUS,
    CHAT,
    CHAT_CONTINUE,
    PING;

    @Getter
    private String username;

    Status() {
    }

    Status(String username) {
        this.username = "status";
    }

    public Status setUsername(String username) {
        this.username = username;
        return this;
    }
}
