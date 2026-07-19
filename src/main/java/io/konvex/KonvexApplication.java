package io.konvex;

import io.konvex.config.MatchingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MatchingProperties.class)
public class KonvexApplication {

	public static void main(String[] args) {
		SpringApplication.run(KonvexApplication.class, args);
	}

}
