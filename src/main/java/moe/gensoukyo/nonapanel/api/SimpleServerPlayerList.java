package moe.gensoukyo.nonapanel.api;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class SimpleServerPlayerList {
    private List<SimpleServerPlayer> playerList = new ArrayList<>();
}

