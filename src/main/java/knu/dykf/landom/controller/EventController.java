package knu.dykf.landom.controller;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import knu.dykf.landom.dto.request.SdkEventRequest;
import knu.dykf.landom.service.EventService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Events", description = "SDK 이벤트 수집 API")
@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 수집", description = "SDK로부터 전달받은 다건의 이벤트를 저장합니다.")
    @PostMapping
    public ResponseEntity<Void> collectEvents(
            @Parameter(description = "프로젝트 API 키", required = true)
            @RequestHeader("X-Project-Key") String projectKey,
            @Valid @RequestBody SdkEventRequest request) {

        eventService.collectEvents(projectKey, request);

        return ResponseEntity.ok().build();
    }
}