# Backend package map

Each feature is a vertical slice. Add HTTP controllers in `controller`, business rules in
`service`, database access in `repository`, JPA entities in `entity`, and API request/
response records in `dto`. Cross-feature code belongs in `common`.

Feature ownership: Developer A owns auth, users, locations, fences, sections, devices,
gateways, configuration, and common. Developer B owns telemetry, dashboard, alerts,
notifications, and reports.
