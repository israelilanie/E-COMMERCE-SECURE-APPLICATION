package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.example.demo.model.persistence.User;
import com.example.demo.model.persistence.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTests {

	@Mock
	private UserRepository userRepository;

	@Test
	void loadUserByUsernameReturnsSpringSecurityUser() {
		User user = new User();
		user.setUsername("security-user");
		user.setPassword("encoded-password");
		when(userRepository.findByUsername("security-user")).thenReturn(user);

		UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);
		UserDetails userDetails = service.loadUserByUsername("security-user");

		assertEquals("security-user", userDetails.getUsername());
		assertEquals("encoded-password", userDetails.getPassword());
	}

	@Test
	void loadUserByUsernameThrowsWhenUserIsMissing() {
		when(userRepository.findByUsername("missing-user")).thenReturn(null);

		UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);

		assertThrows(UsernameNotFoundException.class, () -> service.loadUserByUsername("missing-user"));
	}
}
