package io.konvex.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.konvex.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ApiKeyAuthenticationFilterTest {

	@Test
	void acceptsTheConfiguredApiKey() throws ServletException, IOException {
		SecurityProperties properties = propertiesWithKey("test-key");
		ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "test-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean chainCalled = new AtomicBoolean();

		FilterChain chain = (req, res) -> chainCalled.set(true);
		filter.doFilter(request, response, chain);

		assertEquals(200, response.getStatus());
		assertTrue(chainCalled.get());
	}

	@Test
	void rejectsAnInvalidApiKey() throws ServletException, IOException {
		SecurityProperties properties = propertiesWithKey("test-key");
		ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "wrong-key");
		MockHttpServletResponse response = new MockHttpServletResponse();
		AtomicBoolean chainCalled = new AtomicBoolean();

		FilterChain chain = (req, res) -> chainCalled.set(true);
		filter.doFilter(request, response, chain);

		assertEquals(401, response.getStatus());
		assertFalse(chainCalled.get());
	}

	@Test
	void rejectsRequestsWhenNoApiKeyIsConfigured() throws ServletException, IOException {
		SecurityProperties properties = propertiesWithKey("");
		ApiKeyAuthenticationFilter filter = new ApiKeyAuthenticationFilter(properties);

		MockHttpServletRequest request = new MockHttpServletRequest();
		request.addHeader(ApiKeyAuthenticationFilter.API_KEY_HEADER, "test-key");
		MockHttpServletResponse response = new MockHttpServletResponse();

		FilterChain chain = (req, res) -> {
			throw new AssertionError("chain should not be called");
		};
		filter.doFilter(request, response, chain);

		assertEquals(401, response.getStatus());
	}

	private static SecurityProperties propertiesWithKey(String key) {
		SecurityProperties properties = new SecurityProperties();
		properties.setApiKey(key);
		return properties;
	}
}
