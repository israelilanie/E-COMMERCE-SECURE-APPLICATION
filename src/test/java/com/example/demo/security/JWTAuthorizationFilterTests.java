package com.example.demo.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.Date;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.context.SecurityContextHolder;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import jakarta.servlet.FilterChain;

class JWTAuthorizationFilterTests {

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void continuesWithoutAuthenticatingWhenAuthorizationHeaderIsMissing() throws Exception {
		FilterChain chain = mock(FilterChain.class);

		new JWTAuthorizationFilter(mock(AuthenticationManager.class)).doFilter(
				new MockHttpServletRequest(), new MockHttpServletResponse(), chain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void authenticatesRequestWithValidBearerToken() throws Exception {
		String token = JWT.create()
				.withSubject("token-user")
				.withExpiresAt(new Date(System.currentTimeMillis() + 60_000))
				.sign(Algorithm.HMAC512(SecurityConstants.SECRET));
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token);
		FilterChain chain = mock(FilterChain.class);

		new JWTAuthorizationFilter(mock(AuthenticationManager.class)).doFilter(request, new MockHttpServletResponse(), chain);

		assertEquals("token-user", SecurityContextHolder.getContext().getAuthentication().getName());
		verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}

	@Test
	void ignoresInvalidBearerToken() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + "not-a-token");
		FilterChain chain = mock(FilterChain.class);

		new JWTAuthorizationFilter(mock(AuthenticationManager.class)).doFilter(request, new MockHttpServletResponse(), chain);

		assertNull(SecurityContextHolder.getContext().getAuthentication());
		verify(chain).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
	}
}
