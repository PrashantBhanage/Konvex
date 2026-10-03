package io.konvex.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.konvex.config.SecurityProperties;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
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

		filter.doFilter(
				request,
				response,
				new MockFilterChain((req, res) -> chainCalled.set(true)));

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

		filter.doFilter(
				request,
				response,
				new MockFilterChain((req, res) -> chainCalled.set(true)));

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

		filter.doFilter(request, response, new MockFilterChain());

		assertEquals(401, response.getStatus());
	}

	private static SecurityProperties propertiesWithKey(String key) {
		SecurityProperties properties = new SecurityProperties();
		properties.setApiKey(key);
		return properties;
	}
}
