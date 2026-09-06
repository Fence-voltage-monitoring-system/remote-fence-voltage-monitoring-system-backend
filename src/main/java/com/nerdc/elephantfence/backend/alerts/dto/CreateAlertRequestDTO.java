package com.nerdc.elephantfence.backend.alerts.dto;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;
@Data public class CreateAlertRequestDTO {
    @NotNull private Long fenceId;
    private Long sectionId;
    @NotBlank @Size(max=200) private String title;
    @NotBlank @Pattern(regexp="WIRE_BREAK|DEVICE_OFFLINE|LOW_BATTERY|VOLTAGE_DROP|LOW_VOLTAGE|SOLAR_FAILURE|OTHER") private String type;
    @NotBlank @Pattern(regexp="CRITICAL|WARNING") private String severity;
    @PositiveOrZero @Digits(integer=3,fraction=2) private BigDecimal detectedVoltageKv;
    @PositiveOrZero @Digits(integer=3,fraction=2) private BigDecimal thresholdVoltageKv;
    @NotBlank @Size(max=2000) private String description;
}

