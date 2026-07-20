package io.konvex;

import io.konvex.config.MatchingProperties;
import io.konvex.config.SecurityProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableConfigurationProperties({MatchingProperties.class, SecurityProperties.class})
@EnableScheduling
public class KonvexApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonvexApplication.class, args);
	}

}
