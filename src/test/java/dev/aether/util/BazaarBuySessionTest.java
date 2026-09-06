package dev.aether.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BazaarBuySessionTest {
    @Test
    void clicksTheActualItemOnTheCustomAmountConfirmation() {
        for (String label : new String[]{"§aEnchanted Wheat", "§aBuy Enchanted Wheat", "§aBuy 1,234x Enchanted Wheat"}) {
            var buy = new BazaarBuySession("Enchanted Wheat", 1234);
            var items = List.of(new BazaarBuySession.MenuItem(13, label, false),
                    new BazaarBuySession.MenuItem(11, "Cancel", true),
                    new BazaarBuySession.MenuItem(15, "Go Back", false));
            assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy", items, 0, 300));
            assertEquals(13, buy.confirmationSlot(7, "Confirm Instant Buy", items, 300, 300));
            assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy", items, 1000, 300));
            assertFalse(buy.completed());
        }
    }

    @Test
    void warningUnlocksToAnItemForEveryQuantity() {
        for (int quantity : new int[]{1, 64, 123}) {
            var buy = new BazaarBuySession("Enchanted Wheat", quantity);
            var locked = List.of(new BazaarBuySession.MenuItem(13, "Warning! Wait 5 seconds", true));
            var unlocked = List.of(new BazaarBuySession.MenuItem(13, "Buy " + quantity + "x Enchanted Wheat", false),
                    new BazaarBuySession.MenuItem(11, "Cancel", true));
            assertEquals(-1, buy.confirmationSlot(7, "Bazaar Alert!", locked, 0, 300));
            assertEquals(-1, buy.confirmationSlot(7, "Bazaar Alert!", locked, 5000, 300));
            assertEquals(-1, buy.confirmationSlot(7, "Bazaar Alert!", unlocked, 5050, 300));
            assertEquals(13, buy.confirmationSlot(7, "Bazaar Alert!", unlocked, 5350, 300));
            assertEquals(-1, buy.confirmationSlot(7, "Bazaar Alert!", unlocked, 5700, 300));
        }
    }

    @Test
    void pollsNormalConfirmationThenWarningReusingTheSameSlot() {
        var buy = new BazaarBuySession("Wheat", 123);
        var item = List.of(new BazaarBuySession.MenuItem(13, "Wheat", false));
        assertEquals(13, buy.confirmationSlot(7, "Confirm Instant Buy", item, 0, 0));
        assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy",
                List.of(new BazaarBuySession.MenuItem(13, "Warning!", true)), 100, 300));
        assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy", item, 5000, 300));
        assertEquals(13, buy.confirmationSlot(7, "Confirm Instant Buy", item, 5300, 300));
        buy.onChat("[Bazaar] Bought 123x Wheat for 1,000 coins!");
        assertEquals(-1, buy.confirmationSlot(8, "Confirm Instant Buy", item, 6000, 0));
    }

    @Test
    void ignoresQuantityMenusAndUnrelatedItemsAndResetsDelayWhenLeavingTheDialog() {
        var buy = new BazaarBuySession("Wheat", 123);
        var item = List.of(new BazaarBuySession.MenuItem(13, "Wheat", false));
        assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy", item, 0, 300));
        assertEquals(-1, buy.confirmationSlot(7, "How many do you want?", item, 200, 300));
        assertEquals(-1, buy.confirmationSlot(7, "Confirm Instant Buy", item, 500, 300));
        assertEquals(13, buy.confirmationSlot(7, "Confirm Instant Buy", item, 800, 300));

        for (String name : new String[]{"Cancel", "Cancel Confirmation", "Enchanted Wheat", "Buy 64x Wheat", " "}) {
            assertEquals(-1, buy.confirmationSlot(8, "Confirm Instant Buy",
                    List.of(new BazaarBuySession.MenuItem(13, name, false)), 1000, 0));
        }
    }

    @Test
    void aRememberedWarningCannotAuthorizeAnUnrelatedScreenWithTheSameId() {
        var buy = new BazaarBuySession("Wheat", 1);
        assertEquals(-1, buy.confirmationSlot(7, "Expensive purchase",
                List.of(new BazaarBuySession.MenuItem(13, "Warning!", true)), 0, 0));
        var item = List.of(new BazaarBuySession.MenuItem(13, "Wheat", false));
        assertEquals(-1, buy.confirmationSlot(7, "Bazaar ➜ Wheat", item, 5000, 0));
        assertEquals(13, buy.confirmationSlot(7, "Expensive purchase", item, 5100, 0));
    }

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
