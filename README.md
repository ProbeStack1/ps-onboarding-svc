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

Admin-backend service calls may use a short-lived RS256 service token through the same header.
The token must contain `token_type=probestack_service_access`, a `principal_type` of `SERVICE`
or `USER_DELEGATION`, a configured `client_id`, `organization_id`, and the scope required by the
admin endpoint. Human context tokens continue to use the existing organization-role RBAC path.

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

Service-token policy can be configured with:

```text
ONBOARDING_SERVICE_AUTH_ENABLED=true
ONBOARDING_SERVICE_AUTH_TRUSTED_CLIENTS=probestack-admin-backend
```

Admin endpoint service scopes are:

```text
onboarding:members:read
onboarding:access:read
onboarding:bootstrap:read
onboarding:assignments:read
onboarding:assignments:write
onboarding:business-units:read
onboarding:business-units:write
onboarding:projects:read
onboarding:projects:write
onboarding:applications:read
onboarding:applications:write
onboarding:consumers:read
onboarding:consumers:write
onboarding:developers:read
onboarding:developers:write
onboarding:teams:read
onboarding:teams:write
```

See [Admin panel service-to-service authentication](docs/admin-panel-service-auth.md) for the
token acquisition and request flow.

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

GET    /developers
POST   /developers
GET    /developers/{id}
PATCH  /developers/{id}
DELETE /developers/{id}

GET    /access/teams
POST   /access/teams
GET    /access/teams/{id}
PATCH  /access/teams/{id}
DELETE /access/teams/{id}

GET    /organization-members?status=ACTIVE&page=0&size=20
GET    /organization-members/{principalId}/access

GET    /role-assignments
POST   /role-assignments
GET    /role-assignments/{id}
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
ONBOARDING_ADMIN_USERS_PATH=/api/organizations/%s/users-with-roles
```

The provider response is a non-paginated array. Onboarding forwards `status` in lower case, then
applies search and pagination locally. When the provider omits a user ID, normalized email is used
as the member principal key.

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
