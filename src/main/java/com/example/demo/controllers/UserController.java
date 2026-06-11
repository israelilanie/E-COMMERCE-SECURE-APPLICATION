package com.example.demo.controllers;

import java.math.BigDecimal;
import java.util.ArrayList;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.model.persistence.Cart;
import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.UserRepository;
import com.example.demo.model.requests.CreateUserRequest;

@RestController
@RequestMapping("/api/user")
public class UserController {

	private static final int MIN_PASSWORD_LENGTH = 7;

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public UserController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@GetMapping("/id/{id}")
	public ResponseEntity<User> findById(@PathVariable Long id, Authentication authentication) {
		return userRepository.findById(id)
				.map(user -> isCurrentUser(user.getUsername(), authentication)
						? ResponseEntity.ok(user)
						: ResponseEntity.status(HttpStatus.FORBIDDEN).<User>build())
				.orElseGet(() -> ResponseEntity.notFound().build());
	}
	
	@GetMapping("/{username}")
	public ResponseEntity<User> findByUserName(@PathVariable String username, Authentication authentication) {
		if (!isCurrentUser(username, authentication)) {
			return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
		}
		User user = userRepository.findByUsername(username);
		return user == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(user);
	}
	
	@PostMapping("/create")
	public ResponseEntity<User> createUser(@RequestBody CreateUserRequest createUserRequest) {
		if (createUserRequest.getUsername() == null || createUserRequest.getUsername().isBlank()) {
			return ResponseEntity.badRequest().build();
		}
		if (createUserRequest.getPassword() == null
				|| createUserRequest.getPassword().length() < MIN_PASSWORD_LENGTH
				|| !createUserRequest.getPassword().equals(createUserRequest.getConfirmPassword())) {
			return ResponseEntity.badRequest().build();
		}
		if (userRepository.findByUsername(createUserRequest.getUsername()) != null) {
			return ResponseEntity.status(HttpStatus.CONFLICT).build();
		}

		User user = new User();
		user.setUsername(createUserRequest.getUsername());
		user.setPassword(passwordEncoder.encode(createUserRequest.getPassword()));
		Cart cart = new Cart();
		cart.setItems(new ArrayList<>());
		cart.setTotal(BigDecimal.ZERO);
		cart.setUser(user);
		user.setCart(cart);
		userRepository.save(user);
		return ResponseEntity.ok(user);
	}

	private boolean isCurrentUser(String username, Authentication authentication) {
		return authentication != null && authentication.getName().equals(username);
	}
}
