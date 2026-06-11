package com.example.demo.model.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.junit.jupiter.api.Test;

class UserOrderTests {

	@Test
	void createFromCartCopiesCartValues() {
		User user = new User();
		user.setUsername("order-user");

		Item item = new Item();
		item.setId(1L);
		item.setPrice(new BigDecimal("3.99"));

		Cart cart = new Cart();
		cart.setUser(user);
		cart.setItems(new ArrayList<>());
		cart.addItem(item);

		UserOrder order = UserOrder.createFromCart(cart);

		assertEquals(user, order.getUser());
		assertEquals(new BigDecimal("3.99"), order.getTotal());
		assertEquals(1, order.getItems().size());
	}

	@Test
	void createFromCartHandlesEmptyCart() {
		User user = new User();
		user.setUsername("empty-user");

		Cart cart = new Cart();
		cart.setUser(user);

		UserOrder order = UserOrder.createFromCart(cart);

		assertNotNull(order.getItems());
		assertTrue(order.getItems().isEmpty());
		assertEquals(BigDecimal.ZERO, order.getTotal());
	}
}
