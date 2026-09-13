package com.nerdc.elephantfence.backend.reports.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
public record ReportRequest(@NotNull Template template, @NotNull @Valid Scope scope,
    @NotNull @Valid DateRange dateRange, @NotNull Options options, @NotNull Format format) {
    public enum Template { FENCE_HEALTH, VOLTAGE_PERFORMANCE, ALERT_SUMMARY, DEVICE_STATUS, GATEWAY_CONNECTIVITY, MAINTENANCE }
    public enum Format { PDF, CSV }
    public record Scope(String province, String district, String fence, String section) {}
    public record DateRange(String preset, String from, String to) {}
    public record Options(boolean includeCharts, boolean includeAlertHistory, boolean includeMaintenanceRecords) {}
}
