package com.nerdc.elephantfence.backend.sections.repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public interface SectionTelemetryProjection {
    Long getId();
    Long getDeviceId();
    String getDeviceName();
    String getDeviceSerial();
    BigDecimal getVoltageKv();
    Integer getBattery();
    Integer getSignal();
    OffsetDateTime getRecordedAt();
}
