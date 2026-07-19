package io.konvex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "konvex.security")
public class SecurityProperties {

	/**
	 * Expected value of the {@code X-API-Key} request header for service-to-service calls.
	 */
	private String apiKey = "change-me-override-via-env";

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
}
