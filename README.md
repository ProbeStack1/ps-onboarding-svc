# ps-onboarding-svc

ProbeStack onboarding microservice for business units, projects, applications, consumers, hierarchy views, selectors, and resource audit history.

## Runtime

- Spring Boot 3.2.5
- Java 17
- MongoDB
- Default port: `8084`
- Context path: `/onboarding-api`

Health check:

```http
GET /onboarding-api/actuator/health
```

## Auth and Tenant Model

All onboarding APIs except health and API documentation endpoints require the context token in
the browser cookie used by ProbeStack:

```http
Cookie: ps_auth_token=<context-token>
```

Service clients can alternatively send the standard bearer header:

```http
Authorization: Bearer <context-token>
```

The token is validated by `forge-auth-lib` using:

- Issuer: `https://auth.probestack.io`
- Audience: `probestack-api`
- JWKS: `https://probestack.io/admin-backend/api/public/users/context-token/jwks`

Tenant and actor identity are taken only from validated token claims:

- Organization: `organization_id`
- User ID: `sub`
- Email: `email`
- Name: `name`
- Role: `role`

Legacy `X-Organization-Id` and `X-User-*` headers, and actor fields in request bodies, are not trusted for identity.

## Main APIs

Base URL:

```http
/onboarding-api/api/v1/onboarding
```

Resources:

```http
GET    /business-units
POST   /business-units
GET    /business-units/{id}
PATCH  /business-units/{id}
DELETE /business-units/{id}
GET    /business-units/{id}/history
GET    /business-units/{id}/tree

GET    /projects
POST   /projects
GET    /projects/{id}
PATCH  /projects/{id}
DELETE /projects/{id}
GET    /projects/{id}/history
GET    /projects/{id}/applications

GET    /applications
POST   /applications
GET    /applications/{id}
PATCH  /applications/{id}
DELETE /applications/{id}
PUT    /applications/{id}/consumers
GET    /applications/{id}/history

GET    /consumers
POST   /consumers
GET    /consumers/{id}
PATCH  /consumers/{id}
DELETE /consumers/{id}
GET    /consumers/{id}/history

GET    /organization-members?status=ACTIVE&page=0&size=20
GET    /organization-members/{principalId}/access

GET    /role-assignments
POST   /role-assignments
PATCH  /role-assignments/{id}
DELETE /role-assignments/{id}

GET /admin/access-catalog/users?status=ACTIVE&page=0&size=20
GET /admin/access-catalog/users/{principalId}
GET /admin/access-catalog/resources?resourceType=APPLICATION&page=0&size=20
GET /admin/access-catalog/resources/{resourceType}/{resourceId}
GET /admin/access-catalog/users/{principalId}/bootstrap
```

The organization-member APIs use the admin backend account directory as the canonical member source, then enrich each account with effective onboarding roles from scoped assignments, resource ownership, accepted invitations, teams, and developer profile grants. Complete-directory and role-assignment operations require `ORG_ADMIN` access.

Admin directory integration can be overridden per environment:

```text
ONBOARDING_ADMIN_API_BASE_URL=https://probestack.io/admin-backend
ONBOARDING_ADMIN_ACCOUNTS_PATH=/api/accounts
```

## Admin access catalog

The `/admin/access-catalog/**` endpoints are intended only for the ProbeStack admin backend and admin UI. They require a validated token whose `organization_id` identifies the requested tenant and whose resolved role is `ORG_ADMIN`.

- `users` is the user-centric view: canonical account, every resolved role/source, and effective business-unit, project, and application access.
- `resources` is the product-centric view: every business unit, project, or application with the users who can view/manage it or hold a responsibility/tool role that contributes to it. Use `resourceType=BUSINESS_UNIT`, `PROJECT`, or `APPLICATION`; omit it to page through all three.
- `users/{principalId}/bootstrap` is the login/token integration view. `loginAccess` is suitable for the login API response. `tokenClaims` contains compact snake-case claims suitable for merging into that user's context token.

The admin backend should call the bootstrap endpoint with its validated organization-scoped admin/service context before minting the user context token. Do not add the organization-wide resource matrix to a user's JWT; only merge the returned per-user `tokenClaims`.

Dashboard and selectors:

```http
GET /dashboard/summary
GET /dashboard/hierarchy?page=0&size=20
GET /selectors/business-units?status=ACTIVE
GET /selectors/projects?businessUnitId={id}&status=READY
GET /selectors/consumers?search=team&page=0&size=20
GET /audit?resourceType=BUSINESS_UNIT&resourceId={id}
```

## Notes

- Deletes are soft deletes with `deletedAt`, `deletedBy`, and `DELETED` status.
- Normal lists and selectors exclude soft-deleted records.
- Create, update, status changes, delete, and consumer link changes are written to `onboarding_audit_logs`.
