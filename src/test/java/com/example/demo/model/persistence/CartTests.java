package com.example.demo.model.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class CartTests {

	@Test
	void addAndRemoveItemUpdatesItemsAndTotal() {
		Item item = new Item();
		item.setId(1L);
		item.setPrice(new BigDecimal("2.50"));

		Cart cart = new Cart();
		cart.addItem(item);
		cart.addItem(item);
		cart.removeItem(item);

		assertEquals(1, cart.getItems().size());
		assertEquals(new BigDecimal("2.50"), cart.getTotal());
	}

	@Test
	void removeItemInitializesCollectionAndSubtractsTotal() {
		Item item = new Item();
		item.setId(2L);
		item.setPrice(BigDecimal.ONE);

		Cart cart = new Cart();
		cart.removeItem(item);

		assertTrue(cart.getItems().isEmpty());
		assertEquals(new BigDecimal("-1"), cart.getTotal());
	}
}
