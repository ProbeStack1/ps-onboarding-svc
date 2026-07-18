package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.service.AuditService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/audit")
public class AuditController extends ResponseSupport {
    private final AuditService auditService;
    private final ActorResolver actorResolver;

    public AuditController(AuditService auditService, ActorResolver actorResolver) {
        this.auditService = auditService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<AuditLogResponse>>> history(@RequestParam(required = false) ResourceType resourceType,
                                                                       @RequestParam(required = false) String resourceId,
                                                                       HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        if (resourceType != null && resourceId != null) {
            return ok("Audit history fetched successfully", auditService.history(organizationId, resourceType, resourceId), request);
        }
        return ok("Audit history fetched successfully", auditService.all(organizationId), request);
    }
}
