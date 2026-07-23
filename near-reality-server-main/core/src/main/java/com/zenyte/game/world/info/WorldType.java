package com.zenyte.game.world.info;

/**
 * Stub for the LogoutTransfer packet.
 */
public class WorldType {
    private final String address;
    private final int worldId;
    private final int flag;

    public WorldType(String address, int worldId, int flag) {
        this.address = address;
        this.worldId = worldId;
        this.flag = flag;
    }

    public String getAddress() { return address; }
    public int getWorldId() { return worldId; }
    public int getFlag() { return flag; }
}
