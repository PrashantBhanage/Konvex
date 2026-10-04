package io.konvex.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.konvex.service.HistoryService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class HistoryControllerTest {

	@Test
	void returnsPaginatedMatches() {
		HistoryService service = mock(HistoryService.class);
		HistoryController controller = new HistoryController(service);
		PageResponse<CorrelationMatchResponse> expected = new PageResponse<>(List.of(), 0, 20, 0, 0);
		when(service.findMatches(null, null, null, 0, 20)).thenReturn(expected);

		var response = controller.getMatches(null, null, null, 0, 20);
		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(expected, response.getBody());
	}
}