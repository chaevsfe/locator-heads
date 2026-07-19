package haage;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.Identifier;

public class LocatorHeadsClient implements ClientModInitializer {
    private static final float COMPASS_TOTAL_FOV_DEGREES = 120.0f;

    @Override
    public void onInitializeClient() {
        // Register compass as a last-drawn HUD element so it renders on top of player heads.
        // addLast ensures we draw after all vanilla elements (including INFO_BAR/LocatorBar heads).
        // isHidden() is checked manually below since addLast doesn't inherit a render condition.
        HudElementRegistry.addLast(
            Identifier.fromNamespaceAndPath(LocatorHeads.MOD_ID, "compass"),
            LocatorHeadsClient::renderCompass
        );
    }

    private static void renderCompass(GuiGraphicsExtractor guiGraphics, DeltaTracker deltaTracker) {
        Minecraft minecraft = Minecraft.getInstance();

        if (LocatorHeads.CONFIG == null || !LocatorHeads.CONFIG.enableMod || !LocatorHeads.CONFIG.showCompass) {
            return;
        }

        // Don't render if GUI is hidden (F1)
        if (minecraft.gui.hud.isHidden()) {
            return;
        }

        // Don't render if a screen is open (ESC menu, etc.), except for chat and inventory
        if (minecraft.gui.screen() != null &&
                !(minecraft.gui.screen() instanceof ChatScreen) &&
                !(minecraft.gui.screen() instanceof AbstractContainerScreen)) {
            return;
        }

        // Don't render if in Flashback replay mode
        if (isInFlashbackReplay()) {
            return;
        }

        if (minecraft.level == null || minecraft.getCameraEntity() == null) {
            return;
        }

        float yaw = minecraft.getCameraEntity().getYRot();
        int screenWidth = minecraft.getWindow().getGuiScaledWidth();
        int screenHeight = minecraft.getWindow().getGuiScaledHeight();
        int centerX = screenWidth / 2;
        int compassY = screenHeight - 31;

        if (LocatorHeads.CONFIG.useCoordinatesNotation) {
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "+Z", 0);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "-X", 90);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "-Z", 180);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "+X", 270);
        } else {
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "S", 0);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "W", 90);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "N", 180);
            drawCardinalDirection(guiGraphics, minecraft, centerX, compassY, yaw, "E", 270);
        }
    }

    private static void drawCardinalDirection(GuiGraphicsExtractor guiGraphics, Minecraft minecraft, int centerX, int compassY, float playerYaw, String direction, float directionAngle) {
        float normalizedYaw = ((playerYaw % 360) + 360) % 360;
        float angleDiff = directionAngle - normalizedYaw;

        while (angleDiff > 180) angleDiff -= 360;
        while (angleDiff < -180) angleDiff += 360;

        float halfFov = COMPASS_TOTAL_FOV_DEGREES / 2.0f;

        if (Math.abs(angleDiff) > halfFov) {
            return;
        }

        int xpBarHalfWidth = 91;
        int offset = (int) ((angleDiff / halfFov) * xpBarHalfWidth);
        int x = centerX + offset;

        float centerDistance = Math.abs(angleDiff) / halfFov;
        int alpha = (int) ((1.0f - centerDistance * 0.5f) * 255);

        int compassColorRGB = LocatorHeads.CONFIG.compassColor & 0xFFFFFF;
        int color = (alpha << 24) | compassColorRGB;

        int textX = x - 2;
        if (LocatorHeads.CONFIG.compassShadow) {
            int shadowColor = (alpha << 24);
            guiGraphics.text(minecraft.font, direction, textX - 1, compassY, shadowColor, false);
            guiGraphics.text(minecraft.font, direction, textX + 1, compassY, shadowColor, false);
            guiGraphics.text(minecraft.font, direction, textX, compassY - 1, shadowColor, false);
            guiGraphics.text(minecraft.font, direction, textX, compassY + 1, shadowColor, false);
        }
        guiGraphics.text(minecraft.font, direction, textX, compassY, color, false);
    }

    private static boolean isInFlashbackReplay() {
        try {
            Class<?> flashbackClass = Class.forName("com.moulberry.flashback.Flashback");
            Object isReplaying = flashbackClass.getMethod("isInReplay").invoke(null);
            return (boolean) isReplaying;
        } catch (Exception e) {
            return false;
        }
    }
}
