package io.konvex.security;

import io.konvex.config.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless API-key security for trusted service-to-service callers.
 *
 * <p>Uses a {@link SecurityFilterChain} bean (Spring Security 6+ / Boot 4 style).
 * {@code WebSecurityConfigurerAdapter} is not used — it was removed years ago.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(
			HttpSecurity http,
			SecurityProperties securityProperties) throws Exception {

		ApiKeyAuthenticationFilter apiKeyFilter = new ApiKeyAuthenticationFilter(securityProperties);

		http
				.csrf(AbstractHttpConfigurer::disable)
				.httpBasic(AbstractHttpConfigurer::disable)
				.formLogin(AbstractHttpConfigurer::disable)
				.logout(AbstractHttpConfigurer::disable)
				.sessionManagement(session ->
						session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(auth -> auth
						.requestMatchers("/health").permitAll()
						.anyRequest().authenticated())
				.addFilterBefore(apiKeyFilter, UsernamePasswordAuthenticationFilter.class);

		return http.build();
	}
}
