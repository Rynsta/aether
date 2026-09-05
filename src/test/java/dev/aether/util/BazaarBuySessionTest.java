package dev.aether.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BazaarBuySessionTest {
    @Test
    void waitsForBarrierThenDelayAndConfirmsOnlyOnce() {
        for (int quantity : new int[]{1, 64, 123}) {
            var buy = new BazaarBuySession("Enchanted Wheat", quantity);
            assertFalse(buy.shouldConfirm(7, 13, true, 0, 300));
            assertFalse(buy.shouldConfirm(7, 13, true, 5_000, 300));
            assertFalse(buy.shouldConfirm(7, 13, false, 5_050, 300));
            assertFalse(buy.shouldConfirm(7, 13, false, 5_349, 300));
            assertTrue(buy.shouldConfirm(7, 13, false, 5_350, 300));
            assertFalse(buy.shouldConfirm(7, 13, false, 6_000, 300));
            assertFalse(buy.completed());
            buy.onChat("§6[Bazaar] §aBought " + quantity + "x Enchanted Wheat for 12,345 coins!");
            assertTrue(buy.completed());
        }
    }

    @Test
    void handlesAlertAfterFirstConfirmationInSameMenu() {
        var buy = new BazaarBuySession("Wheat", 64);
        assertTrue(buy.shouldConfirm(7, 13, false, 0, 0));
        assertFalse(buy.shouldConfirm(7, 13, true, 200, 300));
        assertFalse(buy.shouldConfirm(7, -1, false, 4_000, 300));
        assertFalse(buy.shouldConfirm(7, 13, false, 4_050, 300));
        assertTrue(buy.shouldConfirm(7, 13, false, 4_350, 300));
        assertFalse(buy.shouldConfirm(7, 13, false, 4_700, 300));
    }

    @Test
    void aChangedOrEmptyMenuRestartsTheConfirmationDelay() {
        var buy = new BazaarBuySession("Wheat", 1);
        assertFalse(buy.shouldConfirm(2, 13, false, 0, 300));
        assertFalse(buy.shouldConfirm(3, 11, false, 200, 300));
        assertFalse(buy.shouldConfirm(3, -1, false, 400, 300));
        assertFalse(buy.shouldConfirm(3, 11, false, 500, 300));
        assertTrue(buy.shouldConfirm(3, 11, false, 800, 300));
    }

    @Test
    void requiresReceiptForMatchingItemAndQuantity() {
        var buy = new BazaarBuySession("Enchanted Wheat", 1234);
        buy.onChat("[Bazaar] Bought 1,234x Wheat for 1,000 coins!");
        buy.onChat("[Bazaar] Bought 64x Enchanted Wheat for 1,000 coins!");
        buy.onChat("[Bazaar] You don't have enough coins!");
        assertFalse(buy.completed());
        buy.onChat("[Bazaar] Bought 1,234x Enchanted Wheat for 1,000.5 coins!");
        assertTrue(buy.completed());
    }

    @Test
    void recognizesAlertTitlesWithoutTreatingQuantityMenusAsConfirmations() {
        assertTrue(BazaarBuySession.isPurchaseDialog("§cBazaar Alert!"));
        assertTrue(BazaarBuySession.isPurchaseDialog("Confirm Instant Buy"));
        assertTrue(BazaarBuySession.isConfirmation("Confirm Purchase"));
        assertTrue(BazaarBuySession.isConfirmation("Buy Anyway"));
        assertFalse(BazaarBuySession.isPurchaseDialog("How many do you want?"));
        assertFalse(BazaarBuySession.isPurchaseDialog("Bazaar ➜ Wheat"));
        assertFalse(BazaarBuySession.isConfirmation("Cancel"));
    }
}
