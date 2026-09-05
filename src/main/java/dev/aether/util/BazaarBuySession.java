package dev.aether.util;

import java.util.Locale;
import java.util.regex.Pattern;

final class BazaarBuySession {
    private static final Pattern RECEIPT = Pattern.compile(
            "\\[Bazaar] Bought ([\\d,]+)x (.+?) for [\\d,.]+ coins!", Pattern.CASE_INSENSITIVE);

    private final String item;
    private final int count;
    private volatile boolean completed;
    private int clickedMenu = -1;
    private int clickedSlot = -1;
    private int pendingMenu = -1;
    private int pendingSlot = -1;
    private long readyAt;
    private int alertMenu = -1;

    BazaarBuySession(String item, int count) {
        this.item = normalize(item);
        this.count = count;
    }

    void onChat(String text) {
        var receipt = RECEIPT.matcher(TablistUtils.stripColors(text));
        if (receipt.find() && normalize(receipt.group(2)).equals(item)
                && receipt.group(1).replace(",", "").equals(Integer.toString(count))) {
            completed = true;
        }
    }

    boolean completed() { return completed; }

    boolean isAlertMenu(int menu) { return alertMenu == menu; }

    boolean shouldConfirm(int menu, int slot, boolean blocked, long now, long delay) {
        if (blocked || slot < 0) {
            pendingMenu = pendingSlot = -1;
            // A confirmation can open an alert in the same container and slot.
            if (blocked) {
                clickedMenu = clickedSlot = -1;
                alertMenu = menu;
            }
            return false;
        }
        if (menu == clickedMenu && slot == clickedSlot) return false;
        if (menu != pendingMenu || slot != pendingSlot) {
            pendingMenu = menu;
            pendingSlot = slot;
            readyAt = now + delay;
        }
        if (now < readyAt) return false;
        clickedMenu = menu;
        clickedSlot = slot;
        return true;
    }

    static boolean isPurchaseDialog(String title) {
        String plain = normalize(title);
        return plain.contains("confirm instant buy") || plain.contains("bazaar alert")
                || plain.contains("bazaar warning") || plain.contains("confirm purchase");
    }

    static boolean isConfirmation(String name) {
        String plain = normalize(name);
        return plain.contains("confirm") || plain.contains("buy anyway")
                || plain.contains("proceed") || plain.equals("buy") || plain.equals("buy instantly");
    }

    private static String normalize(String text) {
        return TablistUtils.stripColors(text).trim().toLowerCase(Locale.ROOT);
    }
}
