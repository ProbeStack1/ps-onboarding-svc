package io.probestack.onboarding.controller;

import io.probestack.onboarding.dto.common.ApiResponse;
import io.probestack.onboarding.dto.member.MemberAccessResponse;
import io.probestack.onboarding.dto.member.OrganizationMemberResponse;
import io.probestack.onboarding.service.OrganizationMemberService;
import io.probestack.onboarding.util.ActorResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/onboarding/organization-members")
public class OrganizationMemberController extends ResponseSupport {
    private final OrganizationMemberService memberService;
    private final ActorResolver actorResolver;

    public OrganizationMemberController(OrganizationMemberService memberService, ActorResolver actorResolver) {
        this.memberService = memberService;
        this.actorResolver = actorResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrganizationMemberResponse>>> list(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return page("Organization members fetched successfully",
                memberService.list(
                        organizationId,
                        search,
                        status,
                        page,
                        size,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                page,
                size,
                request);
    }

    @GetMapping("/{principalId}/access")
    public ResponseEntity<ApiResponse<MemberAccessResponse>> access(
            @PathVariable String principalId,
            HttpServletRequest request) {
        String organizationId = actorResolver.requireOrganizationId(request);
        return ok("Organization member access fetched successfully",
                memberService.get(
                        organizationId,
                        principalId,
                        request.getHeader(HttpHeaders.AUTHORIZATION),
                        actorResolver.requireActor(null, request)),
                request);
    }
}
