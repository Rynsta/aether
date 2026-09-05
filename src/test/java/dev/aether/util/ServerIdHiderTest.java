package dev.aether.util;

import dev.aether.config.AetherConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class ServerIdHiderTest {
    @Test
    void hidesIdsWithShortDatesLongDatesUnicodeSpacesAndLobbyLabels() {
        for (String text : new String[]{"09/05/26 mini123A", "9/5/2026\u00a0m123AB", "Server: mega12C", "Lobby ID: lobby43", "mini45A"}) {
            String hidden = plain(ServerIdHider.replace(FormattedCharSequence.forward(text, Style.EMPTY), "private"));
            assertTrue(hidden.endsWith("private"), hidden);
            assertFalse(hidden.matches(".*(?:mini|mega|lobby|m)\\d+.*"), hidden);
        }
        assertTrue(ServerIdHider.spans("Purse: 123,456 | 9/5/26 | Level 42").isEmpty());
    }

    @Test
    void preservesTheDateAndFormattingWhenTheIdSpansComponents() {
        var line = Component.literal("09/05/26 ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal("mini12").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.BOLD))
                .append(Component.literal("A").withStyle(ChatFormatting.DARK_GRAY));
        var hidden = ServerIdHider.replace(line.getVisualOrderText(), "private");
        assertEquals("09/05/26 private", plain(hidden));
        var styles = new ArrayList<Style>();
        hidden.accept((index, style, codePoint) -> { styles.add(style); return true; });
        assertEquals(ChatFormatting.GRAY.getColor(), styles.getFirst().getColor().getValue());
        assertTrue(styles.getLast().isBold());
        assertEquals(ChatFormatting.DARK_GRAY.getColor(), styles.getLast().getColor().getValue());
    }

    @Test
    void repeatedTransformsDoNotDuplicateCustomTextOrLoseUnicode() {
        for (String replacement : new String[]{"aether.cat", "Private Server", "[mini123]", "", "\uD83D\uDE80"}) {
            var once = ServerIdHider.replace(FormattedCharSequence.forward("\uD83D\uDE80 09/05/26 mini1A", Style.EMPTY), replacement);
            var twice = ServerIdHider.replace(once, replacement);
            assertEquals("\uD83D\uDE80 09/05/26 " + replacement, plain(once));
            assertEquals(plain(once), plain(twice));
        }
    }

    @Test
    void theExistingHiderAlsoHandlesLegacyFormattingAndStyledComponents() {
        boolean master = AetherConfig.NICK_HIDER_MASTER_ENABLED.get();
        boolean hide = AetherConfig.HIDE_SERVER_ID.get();
        boolean names = AetherConfig.NICK_HIDER_ENABLED.get();
        String replacement = AetherConfig.CUSTOM_SERVER_ID.get();
        try {
            AetherConfig.NICK_HIDER_MASTER_ENABLED.set(true);
            AetherConfig.HIDE_SERVER_ID.set(true);
            AetherConfig.NICK_HIDER_ENABLED.set(false);
            AetherConfig.CUSTOM_SERVER_ID.set("private");
            String hidden = NickHiderUtils.transformString("\u00a7709/05/26 \u00a78mini12\u00a7lA");
            assertEquals("\u00a7709/05/26 \u00a78private", hidden);
            Component original = Component.literal("9/5/2026 ").append(Component.literal("mini12A").withStyle(ChatFormatting.BOLD));
            assertEquals("9/5/2026 private", NickHiderUtils.transformComponent(original).getString());
            AetherConfig.HIDE_SERVER_ID.set(false);
            assertSame(original, NickHiderUtils.transformComponent(original));
        } finally {
            AetherConfig.NICK_HIDER_MASTER_ENABLED.set(master);
            AetherConfig.HIDE_SERVER_ID.set(hide);
            AetherConfig.NICK_HIDER_ENABLED.set(names);
            AetherConfig.CUSTOM_SERVER_ID.set(replacement);
        }
    }

    private static String plain(FormattedCharSequence text) {
        StringBuilder plain = new StringBuilder();
        text.accept((index, style, codePoint) -> { plain.appendCodePoint(codePoint); return true; });
        return plain.toString();
    }
}
