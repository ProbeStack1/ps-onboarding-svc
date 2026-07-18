package io.probestack.onboarding.service;

import io.probestack.onboarding.dto.audit.AuditLogResponse;
import io.probestack.onboarding.dto.common.PagedResult;
import io.probestack.onboarding.dto.consumer.ConsumerCreateRequest;
import io.probestack.onboarding.dto.consumer.ConsumerResponse;
import io.probestack.onboarding.dto.consumer.ConsumerUpdateRequest;
import io.probestack.onboarding.exception.ResourceNotFoundException;
import io.probestack.onboarding.model.AuditAction;
import io.probestack.onboarding.model.Consumer;
import io.probestack.onboarding.model.ConsumerStatus;
import io.probestack.onboarding.model.ResourceType;
import io.probestack.onboarding.repository.ConsumerRepository;
import io.probestack.onboarding.util.ActorResolver;
import io.probestack.onboarding.util.FieldChangeDetector;
import io.probestack.onboarding.util.SlugNormalizer;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class ConsumerService {
    private final ConsumerRepository consumerRepository;
    private final AuditService auditService;
    private final PagingService pagingService;
    private final AccessControlService accessControlService;

    public ConsumerService(ConsumerRepository consumerRepository, AuditService auditService, PagingService pagingService, AccessControlService accessControlService) {
        this.consumerRepository = consumerRepository;
        this.auditService = auditService;
        this.pagingService = pagingService;
        this.accessControlService = accessControlService;
    }

    public ConsumerResponse create(String organizationId, ConsumerCreateRequest request, ActorResolver.Actor actor) {
        if (!accessControlService.canManageConsumerCatalog(organizationId, actor)) {
            accessControlService.requireOrgAdmin(organizationId, actor);
        }
        Consumer consumer = Consumer.builder()
                .organizationId(organizationId)
                .name(request.getName().trim())
                .pocName(SlugNormalizer.trimToNull(request.getPocName()))
                .pocEmail(SlugNormalizer.trimToNull(request.getPocEmail()))
                .smeName(SlugNormalizer.trimToNull(request.getSmeName()))
                .smeEmail(SlugNormalizer.trimToNull(request.getSmeEmail()))
                .consumerConfig(SlugNormalizer.trimToNull(request.getConsumerConfig()))
                .apiTps(request.getApiTps())
                .quotaRequestsPerDay(request.getQuotaRequestsPerDay())
                .rateLimitRequestsPerMinute(request.getRateLimitRequestsPerMinute())
                .apiKeyInformation(SlugNormalizer.trimToNull(request.getApiKeyInformation()))
                .status(request.getStatus() == null ? ConsumerStatus.ACTIVE : request.getStatus())
                .createdBy(actor.name())
                .createdByEmail(actor.email())
                .updatedBy(actor.name())
                .updatedByEmail(actor.email())
                .build();
        Consumer saved = consumerRepository.save(consumer);
        auditService.record(organizationId, ResourceType.CONSUMER, saved.getId(), AuditAction.CREATE, actor, List.of(), null, saved);
        return toResponse(saved);
    }

    public PagedResult<ConsumerResponse> list(String organizationId, String search, ConsumerStatus status, int page, int size, ActorResolver.Actor actor) {
        List<Consumer> source = status == null
                ? consumerRepository.findByOrganizationIdAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId)
                : consumerRepository.findByOrganizationIdAndStatusAndDeletedAtIsNullOrderByUpdatedAtDesc(organizationId, status);
        List<ConsumerResponse> filtered = accessControlService.filterConsumers(organizationId, source, actor).stream()
                .filter(consumer -> matches(search, consumer.getName(), consumer.getPocName(), consumer.getPocEmail(), consumer.getSmeName(), consumer.getSmeEmail()))
                .map(this::toResponse)
                .toList();
        return pagingService.page(filtered, page, size);
    }

    public ConsumerResponse get(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireConsumerView(organizationId, id, actor);
        return toResponse(find(organizationId, id));
    }

    public ConsumerResponse update(String organizationId, String id, ConsumerUpdateRequest request, ActorResolver.Actor actor) {
        accessControlService.requireConsumerManage(organizationId, id, actor);
        Consumer consumer = find(organizationId, id);
        Map<String, Object> before = consumerFields(consumer);
        if (StringUtils.hasText(request.getName())) consumer.setName(request.getName().trim());
        if (request.getPocName() != null) consumer.setPocName(SlugNormalizer.trimToNull(request.getPocName()));
        if (request.getPocEmail() != null) consumer.setPocEmail(SlugNormalizer.trimToNull(request.getPocEmail()));
        if (request.getSmeName() != null) consumer.setSmeName(SlugNormalizer.trimToNull(request.getSmeName()));
        if (request.getSmeEmail() != null) consumer.setSmeEmail(SlugNormalizer.trimToNull(request.getSmeEmail()));
        if (request.getConsumerConfig() != null) consumer.setConsumerConfig(SlugNormalizer.trimToNull(request.getConsumerConfig()));
        if (request.getApiTps() != null) consumer.setApiTps(request.getApiTps());
        if (request.getQuotaRequestsPerDay() != null) consumer.setQuotaRequestsPerDay(request.getQuotaRequestsPerDay());
        if (request.getRateLimitRequestsPerMinute() != null) consumer.setRateLimitRequestsPerMinute(request.getRateLimitRequestsPerMinute());
        if (request.getApiKeyInformation() != null) consumer.setApiKeyInformation(SlugNormalizer.trimToNull(request.getApiKeyInformation()));
        if (request.getStatus() != null) consumer.setStatus(request.getStatus());
        consumer.setUpdatedBy(actor.name());
        consumer.setUpdatedByEmail(actor.email());
        Consumer saved = consumerRepository.save(consumer);
        var changes = FieldChangeDetector.diff(before, consumerFields(saved));
        auditService.record(organizationId, ResourceType.CONSUMER, id, statusChanged(before, saved.getStatus()) ? AuditAction.STATUS_CHANGE : AuditAction.UPDATE, actor, changes, before, saved);
        return toResponse(saved);
    }

    public void delete(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireConsumerManage(organizationId, id, actor);
        Consumer consumer = find(organizationId, id);
        Map<String, Object> before = consumerFields(consumer);
        consumer.setStatus(ConsumerStatus.DELETED);
        consumer.setDeletedAt(Instant.now());
        consumer.setDeletedBy(actor.name());
        consumer.setDeletedByEmail(actor.email());
        consumer.setUpdatedBy(actor.name());
        consumer.setUpdatedByEmail(actor.email());
        Consumer saved = consumerRepository.save(consumer);
        auditService.record(organizationId, ResourceType.CONSUMER, id, AuditAction.DELETE, actor, FieldChangeDetector.diff(before, consumerFields(saved)), before, saved);
    }

    public List<AuditLogResponse> history(String organizationId, String id, ActorResolver.Actor actor) {
        accessControlService.requireConsumerView(organizationId, id, actor);
        find(organizationId, id);
        return auditService.history(organizationId, ResourceType.CONSUMER, id);
    }

    public Consumer find(String organizationId, String id) {
        return consumerRepository.findByIdAndOrganizationIdAndDeletedAtIsNull(id, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Consumer not found: " + id));
    }

    public ConsumerResponse toResponse(Consumer consumer) {
        return ConsumerResponse.builder()
                .id(consumer.getId())
                .organizationId(consumer.getOrganizationId())
                .name(consumer.getName())
                .pocName(consumer.getPocName())
                .pocEmail(consumer.getPocEmail())
                .smeName(consumer.getSmeName())
                .smeEmail(consumer.getSmeEmail())
                .consumerConfig(consumer.getConsumerConfig())
                .apiTps(consumer.getApiTps())
                .quotaRequestsPerDay(consumer.getQuotaRequestsPerDay())
                .rateLimitRequestsPerMinute(consumer.getRateLimitRequestsPerMinute())
                .apiKeyInformation(consumer.getApiKeyInformation())
                .status(consumer.getStatus())
                .createdBy(consumer.getCreatedBy())
                .createdByEmail(consumer.getCreatedByEmail())
                .updatedBy(consumer.getUpdatedBy())
                .updatedByEmail(consumer.getUpdatedByEmail())
                .createdAt(consumer.getCreatedAt())
                .updatedAt(consumer.getUpdatedAt())
                .build();
    }

    private boolean matches(String search, String... values) {
        if (!StringUtils.hasText(search)) return true;
        String needle = search.trim().toLowerCase();
        for (String value : values) {
            if (value != null && value.toLowerCase().contains(needle)) return true;
        }
        return false;
    }

    private boolean statusChanged(Map<String, Object> before, ConsumerStatus status) {
        return !String.valueOf(before.get("status")).equals(String.valueOf(status));
    }

    private Map<String, Object> consumerFields(Consumer consumer) {
        return Map.ofEntries(
                Map.entry("name", nullToEmpty(consumer.getName())),
                Map.entry("pocName", nullToEmpty(consumer.getPocName())),
                Map.entry("pocEmail", nullToEmpty(consumer.getPocEmail())),
                Map.entry("smeName", nullToEmpty(consumer.getSmeName())),
                Map.entry("smeEmail", nullToEmpty(consumer.getSmeEmail())),
                Map.entry("consumerConfig", nullToEmpty(consumer.getConsumerConfig())),
                Map.entry("apiTps", consumer.getApiTps() == null ? "" : consumer.getApiTps()),
                Map.entry("quotaRequestsPerDay", consumer.getQuotaRequestsPerDay() == null ? "" : consumer.getQuotaRequestsPerDay()),
                Map.entry("rateLimitRequestsPerMinute", consumer.getRateLimitRequestsPerMinute() == null ? "" : consumer.getRateLimitRequestsPerMinute()),
                Map.entry("apiKeyInformation", nullToEmpty(consumer.getApiKeyInformation())),
                Map.entry("status", consumer.getStatus())
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}

