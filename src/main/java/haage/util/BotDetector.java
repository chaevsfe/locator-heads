package haage.util;

import java.util.Map;
import java.util.WeakHashMap;

import com.mojang.authlib.GameProfile;

import haage.LocatorHeads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.server.IntegratedServer;
import net.minecraft.core.UUIDUtil;

public final class BotDetector {

    private static final long ZERO_LATENCY_GRACE_MS = 60_000L;
    private static final Map<PlayerInfo, Long> zeroLatencySince = new WeakHashMap<>();
    private static final Map<PlayerInfo, String> loggedDecisions = new WeakHashMap<>();

    private BotDetector() {
    }

    public static boolean isBot(PlayerInfo info) {
        if (info == null) return false;

        Minecraft minecraft = Minecraft.getInstance();
        ClientPacketListener connection = minecraft.getConnection();
        if (connection == null || minecraft.player == null) return false;
        PlayerInfo self = connection.getPlayerInfo(minecraft.player.getUUID());
        if (self == null) return false;
        if (self.getProfile().id().equals(info.getProfile().id())) return false;

        boolean offline = isOfflineAccount(info) && !isOfflineAccount(self);
        boolean lonePlayerWorld = isUnpublishedSingleplayer(minecraft);
        boolean hasSession = info.getChatSession() != null;
        boolean sessionsOnServer = anySessionPresent(connection);
        int latency = info.getLatency();

        if (latency != 0) {
            zeroLatencySince.remove(info);
        } else {
            zeroLatencySince.putIfAbsent(info, System.currentTimeMillis());
        }
        Long zeroSince = zeroLatencySince.get(info);
        boolean sustainedZeroPing = zeroSince != null
                && System.currentTimeMillis() - zeroSince >= ZERO_LATENCY_GRACE_MS;

        boolean bot;
        if (offline || lonePlayerWorld) {
            bot = true;
        } else if (hasSession) {
            bot = false;
        } else if (sessionsOnServer) {
            bot = latency == 0;
        } else {
            bot = sustainedZeroPing;
        }

        String decision = "hidden=" + bot + " offline=" + offline + " lonePlayerWorld=" + lonePlayerWorld
                + " chatSession=" + hasSession + " sessionsOnServer=" + sessionsOnServer
                + " zeroPing=" + (latency == 0) + " sustainedZeroPing=" + sustainedZeroPing;
        if (!decision.equals(loggedDecisions.get(info))) {
            loggedDecisions.put(info, decision);
            LocatorHeads.LOGGER.info("Bot check for {}: {}", info.getProfile().name(), decision);
        }
        return bot;
    }

    private static boolean isOfflineAccount(PlayerInfo info) {
        GameProfile profile = info.getProfile();
        String name = profile.name();
        if (name == null || name.isEmpty()) return false;
        return profile.id().equals(UUIDUtil.createOfflinePlayerUUID(name));
    }

    private static boolean isUnpublishedSingleplayer(Minecraft minecraft) {
        if (!minecraft.hasSingleplayerServer()) return false;
        IntegratedServer server = minecraft.getSingleplayerServer();
        return server != null && !server.isPublished();
    }

    private static boolean anySessionPresent(ClientPacketListener connection) {
        for (PlayerInfo other : connection.getOnlinePlayers()) {
            if (other.getChatSession() != null) return true;
        }
        return false;
    }
}
