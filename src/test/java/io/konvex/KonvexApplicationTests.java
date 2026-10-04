package io.konvex;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:konvex-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"spring.jpa.hibernate.ddl-auto=create-drop",
		"spring.flyway.enabled=false",
		"konvex.opensky.enabled=false",
		"konvex.security.api-key=test-key"
})
class KonvexApplicationTests {

	@Test
	void contextLoads() {
	}
}