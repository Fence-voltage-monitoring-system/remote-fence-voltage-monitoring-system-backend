package com.nerdc.elephantfence.backend.reports.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HistoricalAnalysisResponseDTO {
    private List<SummaryMetricDTO> summaryMetrics;
    private List<VoltageTrendPointDTO> voltageTrend;
    private PowerHealthDTO powerHealth;
    private VoltageEventsDTO voltageEvents;
    private AlertFrequencyDTO alertFrequency;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SummaryMetricDTO {
        private String label;
        private String value;
        private String unit;
        private String tone; // green, amber, red, neutral
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoltageTrendPointDTO {
        private String time;
        private String value;
        private double voltage;
        private int x;
        private int y;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PowerHealthDTO {
        private int currentBatteryPercent;
        private double periodChangePercent;
        private List<Integer> batteryCurve;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class VoltageEventsDTO {
        private long totalVoltageDrops;
        private long significantEvents;
        private double periodChangePercent;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AlertFrequencyDTO {
        private long totalAlerts;
        private String peakHourLabel;
        private double periodChangePercent;
        private List<Integer> hourlyHistogram;
    }
}
