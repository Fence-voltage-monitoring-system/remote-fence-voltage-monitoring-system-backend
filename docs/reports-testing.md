# Reports: local testing

1. Restart the backend normally with its existing database environment. Flyway applies V10 automatically; it adds saved report content and permitted fence IDs to generated_reports.
2. Start or restart the frontend with npm start. Sign in and open Reports.
3. Select Fence Health, select a permitted province/district/fence, and click Preview. Confirm the fence selection matches the Fences page.
4. Select CSV and Generate Report. Wait for READY, then Download. Confirm the file contains your fence's real health and voltage values. No matching data should produce a clearly labelled empty report.
5. Refresh Reports. The same report should remain in history and download with the same contents.
6. Repeat with PDF. Open it in a PDF reader. Enable charts for a record-count overview. Use CSV for Sinhala/Tamil text; the current PDF font uses Latin text.
7. Test Voltage Performance for Today and a custom historical date range. Only stored telemetry in that period should appear. Without device telemetry, an empty result is expected.
8. Create an incident in Alerts, then generate Alert Summary for Today. Confirm its code and current workflow status appear.
9. Perform an incident workflow action and generate Maintenance. Confirm its recorded event appears. This reports incident workflow events, not planned maintenance schedules.
10. Test Device Status and Gateway Connectivity against registered, linked devices/gateways. These are current snapshots, not historical uptime.
11. Enable alert history and maintenance records. Confirm the corresponding additional sections appear in the export.
12. Sign in as another user. Their history must not include your reports. Location dropdowns must contain only permitted fences. A copied download URL must reject access.
13. Try a reversed date range, future dates, or a range longer than 366 days. The API must reject it. Results above 5,000 rows require a narrower selection.

Reports history is private to its creator and paginated in groups of 25. Downloads recheck location permissions.
The existing Historical Analysis API method is preserved separately.

Automated checks:
- Backend: mvn.cmd -Dtest=ReportServiceTest test
- Frontend: npm.cmd test -- --watch=false --include=src/app/pages/reports/**/*.spec.ts
- Frontend build: npm.cmd run build
