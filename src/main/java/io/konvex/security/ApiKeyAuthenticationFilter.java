package io.konvex.security;

import io.konvex.config.SecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Validates the {@code X-API-Key} header against the configured service-to-service key.
 *
 * <p>Not registered as a servlet {@code Filter} bean on its own — it is inserted into the
 * Spring Security filter chain via {@link SecurityConfig}.
 */
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

	public static final String API_KEY_HEADER = "X-API-Key";

	private final SecurityProperties securityProperties;
	private final SecurityContextRepository securityContextRepository =
			new RequestAttributeSecurityContextRepository();

	public ApiKeyAuthenticationFilter(SecurityProperties securityProperties) {
		this.securityProperties = securityProperties;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		String path = request.getRequestURI();
		return "/health".equals(path);
	}

	@Override
	protected void doFilterInternal(
			HttpServletRequest request,
			HttpServletResponse response,
			FilterChain filterChain) throws ServletException, IOException {

		String providedKey = request.getHeader(API_KEY_HEADER);
		String expectedKey = securityProperties.getApiKey();

		if (providedKey == null || !constantTimeEquals(expectedKey, providedKey)) {
			writeUnauthorized(response);
			return;
		}

		UsernamePasswordAuthenticationToken authentication =
				UsernamePasswordAuthenticationToken.authenticated(
						"api-key-client",
						null,
						AuthorityUtils.NO_AUTHORITIES);

		// Spring Security 6+: SecurityContextHolderFilter only *loads* the context;
		// callers must explicitly save it so later filters (AuthorizationFilter) see it.
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		securityContextRepository.saveContext(context, request, response);

		filterChain.doFilter(request, response);
	}

	private static boolean constantTimeEquals(String expected, String provided) {
		byte[] expectedBytes = expected.getBytes(StandardCharsets.UTF_8);
		byte[] providedBytes = provided.getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expectedBytes, providedBytes);
	}

	private static void writeUnauthorized(HttpServletResponse response) throws IOException {
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write(
				"{\"error\":\"Unauthorized\",\"message\":\"Missing or invalid X-API-Key header\"}");
	}
}
