package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.consumer.ConsumerCreateRequest;
import io.probestack.onboarding.dto.consumer.ConsumerResponse;
import io.probestack.onboarding.dto.consumer.ConsumerUpdateRequest;
import io.probestack.onboarding.model.ConsumerStatus;
import io.probestack.onboarding.service.ConsumerService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/consumers")
public class ConsumerController extends ResponseSupport {
    private final ConsumerService consumerService;
    private final ActorResolver actorResolver;

    public ConsumerController(ConsumerService consumerService, ActorResolver actorResolver) {
        this.consumerService = consumerService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ConsumerResponse>> create(@Valid @RequestBody ConsumerCreateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Consumer created successfully", consumerService.create(organizationId, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ConsumerResponse>>> list(@RequestParam(required = false) String search,
                                                                    @RequestParam(required = false) ConsumerStatus status,
                                                                    @RequestParam(defaultValue = "0") int page,
                                                                    @RequestParam(defaultValue = "20") int size,
                                                                    HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Consumers fetched successfully", consumerService.list(organizationId, search, status, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumerResponse>> get(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Consumer fetched successfully", consumerService.get(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ConsumerResponse>> update(@PathVariable String id, @Valid @RequestBody ConsumerUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Consumer updated successfully", consumerService.update(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, @RequestBody(required = false) ConsumerUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        consumerService.delete(organizationId, id, actorResolver.requireActor(body == null ? null : body.getActor(), request));
        return noData("Consumer deleted successfully", request);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Consumer history fetched successfully", consumerService.history(organizationId, id, actorResolver.requireActor(null, request)), request);
    }
}

