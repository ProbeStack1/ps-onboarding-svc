package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitCreateRequest;
import io.probestack.onboarding.dto.businessunit.BusinessUnitResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitTreeResponse;
import io.probestack.onboarding.dto.businessunit.BusinessUnitUpdateRequest;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.model.BusinessUnitStatus;
import io.probestack.onboarding.service.BusinessUnitService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/business-units")
public class BusinessUnitController extends ResponseSupport {
    private final BusinessUnitService businessUnitService;
    private final ActorResolver actorResolver;

    public BusinessUnitController(BusinessUnitService businessUnitService, ActorResolver actorResolver) {
        this.businessUnitService = businessUnitService;
        this.actorResolver = actorResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> create(@Valid @RequestBody BusinessUnitCreateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return created("Business unit created successfully", businessUnitService.create(organizationId, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BusinessUnitResponse>>> list(@RequestParam(required = false) String search,
                                                                        @RequestParam(required = false) BusinessUnitStatus status,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "20") int size,
                                                                        HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Business units fetched successfully", businessUnitService.list(organizationId, search, status, page, size, actorResolver.requireActor(null, request)), page, size, request);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> get(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Business unit fetched successfully", businessUnitService.get(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<BusinessUnitResponse>> update(@PathVariable String id, @Valid @RequestBody BusinessUnitUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Business unit updated successfully", businessUnitService.update(organizationId, id, body, actorResolver.requireActor(body.getActor(), request)), request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id, @RequestBody(required = false) BusinessUnitUpdateRequest body, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        businessUnitService.delete(organizationId, id, actorResolver.requireActor(body == null ? null : body.getActor(), request));
        return noData("Business unit deleted successfully", request);
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Business unit history fetched successfully", businessUnitService.history(organizationId, id, actorResolver.requireActor(null, request)), request);
    }

    @GetMapping("/{id}/tree")
    public ResponseEntity<ApiResponse<BusinessUnitTreeResponse>> tree(@PathVariable String id, HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Business unit tree fetched successfully", businessUnitService.tree(organizationId, id, actorResolver.requireActor(null, request)), request);
    }
}

