# Portfolio Architecture Notes

This document summarizes the main technical decisions behind the AI Car Rental SaaS demo.
The project is a controlled portfolio/demo system, not a commercial production SaaS.

## Multi-Tenancy

- Staff operations are tenant-scoped through the authenticated staff principal.
- Public customer booking resolves tenant ownership through tenant slugs and tenant-safe queries.
- Vehicles, reservations, rentals, payments, insurance packages, invoices, notifications, and settings are tied back to tenant ownership.
- Public vehicle responses avoid exposing sensitive fleet data such as full plate numbers.

## Auth And Sessions

- Staff and customer accounts use separate JWT principal types.
- Access tokens are kept in browser memory.
- Refresh sessions are stored as HttpOnly cookies and rotated through backend refresh endpoints.
- Customer account flows include email verification and single-use password reset tokens.
- Staff invitation flows send one-time setup links instead of exposing initial passwords.

## Reservation And Payment Safety

- Reservation creation validates date ranges, vehicle ownership, vehicle availability, and blocking reservation statuses.
- Critical booking/payment paths use transactional service boundaries.
- Pessimistic locking is used around vehicle and payment-sensitive reservation reads to reduce double-booking and duplicate-payment race conditions.
- Payment processing is intentionally simulated through a `PaymentProvider` abstraction.
- The demo provider does not process or store real card data, but the domain still records idempotency keys, provider transaction ids, audit logs, and outbox events.

## Outbox And Kafka

- Business transactions write domain events to the outbox table in the same database transaction as the state change.
- A scheduler publishes pending outbox messages to Kafka.
- Kafka consumers handle downstream workflows such as notifications.
- Failed publish attempts are retried with bounded retry metadata and failed statuses, so asynchronous work is observable and recoverable.

## Redis

- Redis is used for cache-oriented runtime concerns and public AI search rate limiting.
- Test profile uses simple in-memory cache configuration so CI does not depend on a live Redis instance.
- Tenant/settings-related cache invalidation is handled from application services after relevant updates.

## AI Usage

- AI pricing produces pricing recommendations that require staff approval before changing vehicle prices.
- Marketplace natural-language search extracts structured criteria and semantic intent from Turkish or English user input.
- AI never invents available vehicles; PostgreSQL availability queries remain the source of truth.
- Rule-based fallback behavior keeps manual marketplace filtering usable when Gemini is unavailable.

## Demo Deployment Boundary

- Public demo deployments should use the `demo` Spring profile.
- API documentation is disabled by default outside local development.
- Staff/admin credentials should not be published publicly.
- Real payment providers, PCI/card processing, Kubernetes, multi-region observability, and custom model training are intentionally out of scope.
