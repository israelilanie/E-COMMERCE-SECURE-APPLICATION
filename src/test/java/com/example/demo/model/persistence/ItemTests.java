package com.example.demo.model.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class ItemTests {

	@Test
	void equalsAndHashCodeUseId() {
		Item left = new Item();
		left.setId(1L);
		left.setName("Left");
		left.setPrice(new BigDecimal("1.00"));
		left.setDescription("left");

		Item sameId = new Item();
		sameId.setId(1L);

		Item differentId = new Item();
		differentId.setId(2L);

		assertEquals(left, left);
		assertEquals(left, sameId);
		assertEquals(left.hashCode(), sameId.hashCode());
		assertNotEquals(left, differentId);
		assertNotEquals(left, null);
		assertNotEquals(left, "item");
	}

	@Test
	void gettersAndSettersRoundTripValues() {
		Item item = new Item();
		item.setId(3L);
		item.setName("Square Widget");
		item.setPrice(new BigDecimal("1.99"));
		item.setDescription("desc");

		assertEquals(3L, item.getId());
		assertEquals("Square Widget", item.getName());
		assertEquals(new BigDecimal("1.99"), item.getPrice());
		assertEquals("desc", item.getDescription());
		assertTrue(item.equals(item));
		assertFalse(item.equals(new Object()));
	}
}
