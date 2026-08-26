package haage.util;

import com.mojang.authlib.GameProfile;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.core.UUIDUtil;

public final class BotDetector {

    private BotDetector() {
    }

    public static boolean isBot(PlayerInfo info) {
        if (info == null) return false;

        PlayerInfo self = selfInfo();
        if (self == null) return false;
        if (self.getProfile().id().equals(info.getProfile().id())) return false;

        if (isOfflineAccount(info) && !isOfflineAccount(self)) return true;

        return self.getChatSession() != null
                && info.getChatSession() == null
                && info.getLatency() == 0;
    }

    private static boolean isOfflineAccount(PlayerInfo info) {
        GameProfile profile = info.getProfile();
        String name = profile.name();
        if (name == null || name.isEmpty()) return false;
        return profile.id().equals(UUIDUtil.createOfflinePlayerUUID(name));
    }

    private static PlayerInfo selfInfo() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) return null;
        return connection.getPlayerInfo(minecraft.player.getUUID());
    }
}
