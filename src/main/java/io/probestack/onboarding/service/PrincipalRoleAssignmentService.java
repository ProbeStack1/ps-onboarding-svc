package io.probestack.onboarding.service;

import io.probestack.onboarding.client.OrganizationMemberClient;
import io.probestack.onboarding.client.OrganizationMemberRecord;
import io.probestack.onboarding.dto.member.MemberRoleAssignmentResponse;
import io.probestack.onboarding.dto.member.RoleAssignmentCreateRequest;
import io.probestack.onboarding.dto.member.RoleAssignmentUpdateRequest;
import io.probestack.onboarding.exception.DuplicateResourceException;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.*;
import io.probestack.onboarding.repository.PrincipalRoleAssignmentRepository;
import io.probestack.onboarding.util.ActorResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class PrincipalRoleAssignmentService {
    private final PrincipalRoleAssignmentRepository repository;
    private final AccessControlService accessControlService;
    private final AuditService auditService;
    private final OrganizationMemberClient memberClient;

    public PrincipalRoleAssignmentService(
            PrincipalRoleAssignmentRepository repository,
            AccessControlService accessControlService,
            AuditService auditService,
            OrganizationMemberClient memberClient) {
        this.repository = repository;
        this.accessControlService = accessControlService;
        this.auditService = auditService;
        this.memberClient = memberClient;
    }

    public List<MemberRoleAssignmentResponse> list(String organizationId, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId).stream()
                .map(this::toResponse)
                .toList();
    }

    public MemberRoleAssignmentResponse create(
            String organizationId,
            RoleAssignmentCreateRequest request,
            String authorization,
            ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        validateDates(request.getValidFrom(), request.getValidTo());
        String scopeType = normalizeCode(request.getScopeType());
        String scopeId = required(request.getScopeId(), "Scope id is required");
        if ("ORGANIZATION".equals(scopeType) && !organizationId.equals(scopeId)) {
            throw new IllegalArgumentException("Organization-scoped assignments must use the authenticated organization id");
        }
        String roleCode = normalizeCode(request.getRoleCode());
        validateAccessRole(request.getRoleKind(), roleCode, scopeType);
        OrganizationMemberRecord member = memberClient.findMember(
                        organizationId,
                        required(request.getPrincipalId(), "Principal id is required"),
                        authorization)
                .filter(OrganizationMemberRecord::active)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Active organization member not found: " + request.getPrincipalId()));
        if (repository.existsByOrganizationIdAndPrincipalIdAndRoleKindAndRoleCodeAndScopeTypeAndScopeIdAndActiveTrue(
                organizationId, member.principalId(), request.getRoleKind(), roleCode, scopeType, scopeId)) {
            throw new DuplicateResourceException("The member already has this active role assignment");
        }
        PrincipalRoleAssignment saved = repository.save(PrincipalRoleAssignment.builder()
                .organizationId(organizationId)
                .principalId(member.principalId())
                .principalEmail(normalizeEmail(member.email()))
                .principalName(trimToNull(member.name()))
                .roleKind(request.getRoleKind())
                .roleCode(roleCode)
                .scopeType(scopeType)
                .scopeId(scopeId)
                .sourceType(AssignmentSourceType.PRINCIPAL_ASSIGNMENT)
                .active(true)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .build());
        auditService.record(organizationId, ResourceType.PRINCIPAL_ROLE_ASSIGNMENT, saved.getId(),
                AuditAction.CREATE, actor, List.of(), null, saved);
        return toResponse(saved);
    }

    public MemberRoleAssignmentResponse update(
            String organizationId,
            String id,
            RoleAssignmentUpdateRequest request,
            ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        PrincipalRoleAssignment assignment = find(organizationId, id);
        PrincipalRoleAssignment before = copy(assignment);
        Instant validFrom = request.getValidFrom() == null ? assignment.getValidFrom() : request.getValidFrom();
        Instant validTo = request.getValidTo() == null ? assignment.getValidTo() : request.getValidTo();
        validateDates(validFrom, validTo);
        if (request.getActive() != null) assignment.setActive(request.getActive());
        if (request.getValidFrom() != null) assignment.setValidFrom(request.getValidFrom());
        if (request.getValidTo() != null) assignment.setValidTo(request.getValidTo());
        assignment.setUpdatedBy(actor.name());
        assignment.setUpdatedByEmail(actor.email());
        PrincipalRoleAssignment saved = repository.save(assignment);
        auditService.record(organizationId, ResourceType.PRINCIPAL_ROLE_ASSIGNMENT, saved.getId(),
                AuditAction.UPDATE, actor, List.of(), before, saved);
        return toResponse(saved);
    }

    public void revoke(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireOrgAdmin(organizationId, actor);
        PrincipalRoleAssignment assignment = find(organizationId, id);
        PrincipalRoleAssignment before = copy(assignment);
        assignment.setActive(false);
        assignment.setRevokedAt(Instant.now());
        assignment.setRevokedBy(actor.name());
        assignment.setRevokedByEmail(actor.email());
        assignment.setUpdatedBy(actor.name());
        assignment.setUpdatedByEmail(actor.email());
        PrincipalRoleAssignment saved = repository.save(assignment);
        auditService.record(organizationId, ResourceType.PRINCIPAL_ROLE_ASSIGNMENT, saved.getId(),
                AuditAction.DELETE, actor, List.of(), before, saved);
    }

    private PrincipalRoleAssignment find(String organizationId, String id) {
        return repository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Role assignment not found: " + id));
    }

    private void validateAccessRole(RoleKind roleKind, String roleCode, String scopeType) {
        if (roleKind != RoleKind.ACCESS && roleKind != RoleKind.ORGANIZATION) return;
        String code = normalizeCode(roleCode);
        if (!Set.of("ORG_ADMIN", "BUSINESS_UNIT_ADMIN", "PROJECT_ADMIN", "APPLICATION_OWNER", "APPLICATION_MEMBER")
                .contains(code)) {
            throw new IllegalArgumentException("Unsupported enforceable role code: " + code);
        }
        boolean valid = ("ORG_ADMIN".equals(code) && "ORGANIZATION".equals(scopeType))
                || ("BUSINESS_UNIT_ADMIN".equals(code) && "BUSINESS_UNIT".equals(scopeType))
                || ("PROJECT_ADMIN".equals(code) && "PROJECT".equals(scopeType))
                || (Set.of("APPLICATION_OWNER", "APPLICATION_MEMBER").contains(code) && "APPLICATION".equals(scopeType));
        if (!valid) throw new IllegalArgumentException("Role code does not match the requested scope type");
    }

    private void validateDates(Instant validFrom, Instant validTo) {
        if (validFrom != null && validTo != null && !validTo.isAfter(validFrom)) {
            throw new IllegalArgumentException("validTo must be after validFrom");
        }
    }

    private MemberRoleAssignmentResponse toResponse(PrincipalRoleAssignment assignment) {
        Instant now = Instant.now();
        boolean effective = assignment.isActive()
                && (assignment.getValidFrom() == null || !assignment.getValidFrom().isAfter(now))
                && (assignment.getValidTo() == null || assignment.getValidTo().isAfter(now));
        return MemberRoleAssignmentResponse.builder()
                .id(assignment.getId())
                .roleKind(assignment.getRoleKind())
                .roleCode(assignment.getRoleCode())
                .scopeType(assignment.getScopeType())
                .scopeId(assignment.getScopeId())
                .sourceType(assignment.getSourceType())
                .sourceId(assignment.getSourceId())
                .inheritedFrom(assignment.getInheritedFrom())
                .active(assignment.isActive())
                .effective(effective)
                .permissions(List.of())
                .validFrom(assignment.getValidFrom())
                .validTo(assignment.getValidTo())
                .build();
    }

    private PrincipalRoleAssignment copy(PrincipalRoleAssignment source) {
        return PrincipalRoleAssignment.builder()
                .id(source.getId())
                .organizationId(source.getOrganizationId())
                .principalId(source.getPrincipalId())
                .principalEmail(source.getPrincipalEmail())
                .principalName(source.getPrincipalName())
                .roleKind(source.getRoleKind())
                .roleCode(source.getRoleCode())
                .scopeType(source.getScopeType())
                .scopeId(source.getScopeId())
                .sourceType(source.getSourceType())
                .sourceId(source.getSourceId())
                .inheritedFrom(source.getInheritedFrom())
                .active(source.isActive())
                .validFrom(source.getValidFrom())
                .validTo(source.getValidTo())
                .createdBy(source.getCreatedBy())
                .createdByEmail(source.getCreatedByEmail())
                .updatedBy(source.getUpdatedBy())
                .updatedByEmail(source.getUpdatedByEmail())
                .createdAt(source.getCreatedAt())
                .updatedAt(source.getUpdatedAt())
                .build();
    }

    private String required(String value, String message) {
        String normalized = trimToNull(value);
        if (normalized == null) throw new IllegalArgumentException(message);
        return normalized;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeEmail(String value) {
        String email = trimToNull(value);
        return email == null ? null : email.toLowerCase(Locale.ROOT);
    }

    private String normalizeCode(String value) {
        return required(value, "Role or scope code is required")
                .replaceAll("[-\\s]+", "_")
                .toUpperCase(Locale.ROOT);
    }
}
