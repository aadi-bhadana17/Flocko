package com.kilgore.fooddeliveryapp.model;

import com.kilgore.fooddeliveryapp.catalog.model.Addon;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AddonTest {

    @Test
    void testAddonCreationAndGetters() {
        Addon addon = new Addon();

        addon.setAddonId(1L);
        addon.setAddonName("Extra Cheese");
        addon.setPrice(new BigDecimal("50.00"));
        addon.setAvailable(true);

        assertEquals(1L, addon.getAddonId());
        assertEquals("Extra Cheese", addon.getAddonName());
        assertEquals(new BigDecimal("50.00"), addon.getPrice());
        assertTrue(addon.isAvailable());
    }

    @Test
    void testEqualsAndHashCode() {
        Addon addon1 = new Addon();
        addon1.setAddonId(1L);

        Addon addon2 = new Addon();
        addon2.setAddonId(1L);

        Addon addon3 = new Addon();
        addon3.setAddonId(2L);

        assertEquals(addon1, addon2);     // same ID → equal
        assertNotEquals(addon1, addon3);  // different ID → not equal
        assertEquals(addon1.hashCode(), addon2.hashCode());
    }

    @Test
    void testDefaultValues() {
        Addon addon = new Addon();

        assertNull(addon.getAddonId());
        assertNull(addon.getAddonName());
        assertNotNull(addon.getCategories()); // initialized
        assertTrue(addon.getCategories().isEmpty());
        assertFalse(addon.isAvailable()); // default boolean
    }
}