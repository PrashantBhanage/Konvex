package io.konvex.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "konvex.opensky")
public class OpenSkyProperties {

	private String baseUrl = "https://opensky-network.org";
	private boolean enabled = true;
	private long pollIntervalMs = 30_000L;
	private long initialDelayMs = 5_000L;

	public String getBaseUrl() {
		return baseUrl;
	}

	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public long getPollIntervalMs() {
		return pollIntervalMs;
	}

	public void setPollIntervalMs(long pollIntervalMs) {
		this.pollIntervalMs = pollIntervalMs;
	}

	public long getInitialDelayMs() {
		return initialDelayMs;
	}

	public void setInitialDelayMs(long initialDelayMs) {
		this.initialDelayMs = initialDelayMs;
	}
}
