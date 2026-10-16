package com.nerdc.elephantfence.backend.alerts.service;
import com.nerdc.elephantfence.backend.alerts.dto.*;
import com.nerdc.elephantfence.backend.alerts.entity.*;
import com.nerdc.elephantfence.backend.alerts.repository.*;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.gateways.entity.Gateway;
import com.nerdc.elephantfence.backend.gateways.service.GatewayService;
import com.nerdc.elephantfence.backend.sections.entity.Section;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.*;
import com.nerdc.elephantfence.backend.users.entity.*;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.notifications.service.NotificationService;
import org.junit.jupiter.api.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;
import java.util.*;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlertServiceTest {
    AlertRepository alerts=mock(AlertRepository.class);
    AlertEventRepository events=mock(AlertEventRepository.class);
    AlertCommentRepository comments=mock(AlertCommentRepository.class);
    FenceRepository fences=mock(FenceRepository.class);
    SectionRepository sections=mock(SectionRepository.class);
    ProvinceRepository provinces=mock(ProvinceRepository.class);
    DistrictRepository districts=mock(DistrictRepository.class);
    UserRepository users=mock(UserRepository.class);
    AlertRules rules=mock(AlertRules.class);
    NotificationService notifications=mock(NotificationService.class);
    AlertWebSocketHandler live=mock(AlertWebSocketHandler.class);
    AlertAccess access=new AlertAccess(users);
    AlertService service=new AlertService(alerts,events,comments,fences,sections,provinces,districts,users,access,rules,notifications,live);
    User admin=User.builder().id(UUID.randomUUID()).fullName("Admin").email("admin@test.invalid").role(Role.SUPER_ADMIN).enabled(true).build();
    User worker=User.builder().id(UUID.randomUUID()).fullName("Worker").email("worker@test.invalid").role(Role.MAINTENANCE).enabled(true).build();
    Alert alert;
    void login(User user){
        when(users.findByIdWithProvincesAndDistricts(user.getId())).thenReturn(Optional.of(user));
        when(users.findById(user.getId())).thenReturn(Optional.of(user));
        var principal=UserPrincipal.create(user);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
    }
    @BeforeEach void setup(){
        alert=Alert.builder().id(1L).code("ALT-TEST").title("Test incident").severity("WARNING").type("LOW_VOLTAGE").build();
        when(alerts.findById(1L)).thenReturn(Optional.of(alert));
        when(rules.number("healthyReadingsRequired",2)).thenReturn(2);
        when(rules.number("maintenanceAcceptanceTimeoutMinutes",15)).thenReturn(15);
        lenient().when(rules.categoryEnabled(any())).thenReturn(true);
        lenient().when(rules.inAppEnabled()).thenReturn(true);
        lenient().when(rules.smsEnabled()).thenReturn(true);
        lenient().when(rules.websocketEnabled()).thenReturn(true);
        login(admin);
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();}
    @Test void acknowledgementPersistsAuditAndCannotRepeat(){
        assertThat(service.acknowledge(1L).getStatus()).isEqualTo("ACKNOWLEDGED");
        assertThat(alert.getAcknowledgedByUserId()).isEqualTo(admin.getId());
        verify(events).save(argThat(e->e.getEventType().equals("ACKNOWLEDGED")));
        assertThatThrownBy(()->service.acknowledge(1L)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void unassignedWorkerCannotReadIncident(){
        login(worker);
        assertThatThrownBy(()->service.getAlert("1")).isInstanceOf(ResponseStatusException.class);
        verify(alerts,never()).saveAndFlush(any());
    }
    @Test void onlyAssigneeCanAccept(){
        alert.setAssigneeUserId(worker.getId());alert.setAssignmentStatus("AWAITING_ACCEPTANCE");
        assertThatThrownBy(()->service.acceptAssignment(1L)).isInstanceOf(ResponseStatusException.class);
        login(worker);
        assertThat(service.acceptAssignment(1L).getAssignmentStatus()).isEqualTo("ACCEPTED");
        assertThat(alert.getStatus()).isEqualTo("ASSIGNED");
        assertThat(alert.getAcceptanceDeadline()).isNull();
    }
    @Test void cannotAcceptExpiredAssignment(){
        alert.setAssigneeUserId(worker.getId());alert.setAssignmentStatus("AWAITING_ACCEPTANCE");alert.setAcceptanceDeadline(OffsetDateTime.now().minusSeconds(1));login(worker);
        assertThatThrownBy(()->service.acceptAssignment(1L)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void completeRequiresStartedWorkAndRecordsAllFields(){
        alert.setAssigneeUserId(worker.getId());alert.setAssignmentStatus("ACCEPTED");login(worker);
        CompleteWorkRequestDTO dto=new CompleteWorkRequestDTO();dto.setCause("Broken wire");dto.setActions("Replaced wire");dto.setSummary("Repair finished");
        assertThatThrownBy(()->service.completeWork(1L,dto)).isInstanceOf(ResponseStatusException.class);
        service.startWork(1L);
        var response=service.completeWork(1L,dto);
        assertThat(response.getStatus()).isEqualTo("UNDER_MAINTENANCE");
        assertThat(response.getResolutionSummary()).isEqualTo("Repair finished");
        assertThat(response.getAllowedActions()).doesNotContain("START","COMPLETE","RESOLVE");
    }
    @Test void adminResolutionClearsDeadlineAndPreventsReopening(){
        alert.setAcceptanceDeadline(OffsetDateTime.now().plusMinutes(10));
        ResolveManuallyRequestDTO dto=new ResolveManuallyRequestDTO();dto.setReason("Verified repair");
        service.resolveManually(1L,dto);
        assertThat(alert.getStatus()).isEqualTo("RESOLVED");
        assertThat(alert.getAcceptanceDeadline()).isNull();
        assertThatThrownBy(()->service.acknowledge(1L)).isInstanceOf(ResponseStatusException.class);
    }
    @Test void invalidAssigneeNeverCreatesAccounts(){
        ReassignAlertRequestDTO dto=new ReassignAlertRequestDTO();dto.setStaffId(UUID.randomUUID());dto.setReason("Assign");
        assertThatThrownBy(()->service.reassignMaintenance(1L,dto)).isInstanceOf(IllegalArgumentException.class);
        verify(users,never()).save(any());
    }
    @Test void expiryEscalatesToEnabledBackup(){
        alert.setFenceId(4L);alert.setAssigneeUserId(worker.getId());alert.setAssignmentStatus("AWAITING_ACCEPTANCE");alert.setAcceptanceDeadline(OffsetDateTime.now().minusSeconds(1));
        User backup=User.builder().id(UUID.randomUUID()).fullName("Backup").role(Role.MAINTENANCE).enabled(true).build();
        when(fences.findById(4L)).thenReturn(Optional.of(Fence.builder().id(4L).backupMaintenanceUsers(Set.of(backup)).build()));
        when(rules.enabled("escalationEnabled",true)).thenReturn(true);
        service.expireAssignment(1L);
        assertThat(alert.getAssigneeUserId()).isEqualTo(backup.getId());
        assertThat(alert.getAssignmentStatus()).isEqualTo("AWAITING_ACCEPTANCE");
        assertThat(alert.getAcceptanceDeadline()).isAfter(OffsetDateTime.now());
    }
    @Test void unauthenticatedNeverCreatesAdmin(){
        SecurityContextHolder.clearContext();
        assertThatThrownBy(()->service.getStats()).isInstanceOf(ResponseStatusException.class);
        verify(users,never()).save(any());
    }
    @Test void rejectsInvalidPagination(){
        assertThatThrownBy(()->service.getAlerts(new AlertFilters(),0,20)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(()->service.getAlerts(new AlertFilters(),1,1000)).isInstanceOf(IllegalArgumentException.class);
    }
    @Test void wireBreakCreatesCriticalAlert() {
        Section section = Section.builder().id(10L).code("SEC-01").build();
        Province prov = Province.builder().id(1L).name("Western").build();
        District dist = District.builder().id(1L).name("Colombo").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").province(prov).district(dist).primaryMaintenanceUser(worker).build();

        when(rules.wireBreakEnabled()).thenReturn(true);
        when(rules.voltage("criticalKv", 1.5)).thenReturn(1.5);
        when(alerts.existsBySectionIdAndTypeAndStatusNot(10L, "WIRE_BREAK", "RESOLVED")).thenReturn(false);
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(99L);
            return a;
        });

        service.processTelemetry(section, fence, 0.0, 95);

        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "WIRE_BREAK".equals(a.getType()) && "CRITICAL".equals(a.getSeverity())));
    }
    @Test void autoResolveRecoversAlertWhenHealthyReadingsAchieved() {
        Section section = Section.builder().id(10L).code("SEC-01").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").build();
        Alert existingAlert = Alert.builder().id(50L).type("LOW_VOLTAGE").status("UNDER_MAINTENANCE").healthyReadingsReceived(1).build();

        when(rules.autoResolve()).thenReturn(true);
        when(rules.voltage("healthyKv", 5.0)).thenReturn(5.0);
        when(rules.voltage("lowBatteryPercent", 20.0)).thenReturn(20.0);
        when(rules.healthyReadingsRequired()).thenReturn(2);
        when(alerts.findBySectionIdAndStatusNot(10L, "RESOLVED")).thenReturn(List.of(existingAlert));

        service.processTelemetry(section, fence, 6.5, 90);

        assertThat(existingAlert.getStatus()).isEqualTo("RESOLVED");
        assertThat(existingAlert.getResolutionType()).isEqualTo("AUTO_RECOVERY");
        assertThat(existingAlert.getHealthyReadingsReceived()).isEqualTo(2);
        verify(events).save(argThat(e -> "ALERT_RESOLVED".equals(e.getEventType())));
    }
    @Test void gatewayOfflineCreatesCriticalAlert() {
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();
        Gateway gateway = Gateway.builder().id(5L).name("GTW-01").serial("GW-001").fences(Set.of(fence)).build();
        GatewayService gatewayService = mock(GatewayService.class);

        when(rules.gatewayOfflineEnabled()).thenReturn(true);
        when(rules.cooldownMinutes()).thenReturn(30);
        when(alerts.existsByFenceIdAndTypeAndStatusNot(20L, "DEVICE_OFFLINE", "RESOLVED")).thenReturn(false);
        when(gatewayService.computeGatewayStatus(gateway)).thenReturn("offline");
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(101L);
            return a;
        });

        service.checkGatewaysOffline(List.of(gateway), gatewayService);

        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "DEVICE_OFFLINE".equals(a.getType()) && "CRITICAL".equals(a.getSeverity())));
    }
    @Test void gatewayOnlineRecoversAlert() {
        Fence fence = Fence.builder().id(20L).name("Test Fence").build();
        Gateway gateway = Gateway.builder().id(5L).name("GTW-01").serial("GW-001").fences(Set.of(fence)).build();
        GatewayService gatewayService = mock(GatewayService.class);
        Alert offlineAlert = Alert.builder().id(99L).type("DEVICE_OFFLINE").status("UNACKNOWLEDGED").build();

        when(rules.autoResolve()).thenReturn(true);
        when(alerts.findByFenceIdAndTypeAndStatusNot(20L, "DEVICE_OFFLINE", "RESOLVED")).thenReturn(List.of(offlineAlert));
        when(gatewayService.computeGatewayStatus(gateway)).thenReturn("online");

        service.checkGatewaysOffline(List.of(gateway), gatewayService);

        assertThat(offlineAlert.getStatus()).isEqualTo("RESOLVED");
        assertThat(offlineAlert.getResolutionType()).isEqualTo("AUTO_RECOVERY");
        verify(events).save(argThat(e -> "ALERT_RESOLVED".equals(e.getEventType())));
    }
    @Test void criticalBatteryCreatesCriticalAlert() {
        Section section = Section.builder().id(10L).code("SEC-01").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();

        when(rules.criticalBatteryEnabled()).thenReturn(true);
        when(rules.criticalBatteryPercent()).thenReturn(10.0);
        when(rules.voltage("criticalKv", 1.5)).thenReturn(1.5);
        when(rules.voltage("warningKv", 3.0)).thenReturn(3.0);
        when(alerts.existsBySectionIdAndTypeAndStatusNot(10L, "LOW_BATTERY", "RESOLVED")).thenReturn(false);
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(102L);
            return a;
        });

        service.processTelemetry(section, fence, 6.0, 8);

        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "LOW_BATTERY".equals(a.getType()) && "CRITICAL".equals(a.getSeverity()) && a.getThresholdVoltageKv().doubleValue() == 10.0));
    }
    @Test void solarFailureCreatesWarningAlert() {
        Section section = Section.builder().id(10L).code("SEC-01").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();

        when(rules.solarFailureEnabled()).thenReturn(true);
        when(alerts.existsBySectionIdAndTypeAndStatusNot(10L, "SOLAR_FAILURE", "RESOLVED")).thenReturn(false);
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> {
            Alert a = inv.getArgument(0);
            a.setId(103L);
            return a;
        });

        service.processTelemetry(section, fence, 6.0, 80, 0);

        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "SOLAR_FAILURE".equals(a.getType()) && "WARNING".equals(a.getSeverity())));
    }
    @Test void solarFailureAutoRecoversWhenSolarReturns() {
        Section section = Section.builder().id(10L).code("SEC-01").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").build();
        Alert solarAlert = Alert.builder().id(60L).type("SOLAR_FAILURE").status("UNACKNOWLEDGED").healthyReadingsReceived(1).build();

        when(rules.autoResolve()).thenReturn(true);
        when(rules.healthyReadingsRequired()).thenReturn(2);
        when(alerts.findBySectionIdAndStatusNot(10L, "RESOLVED")).thenReturn(List.of(solarAlert));

        service.processTelemetry(section, fence, 6.0, 80, 45);

        assertThat(solarAlert.getStatus()).isEqualTo("RESOLVED");
        assertThat(solarAlert.getResolutionType()).isEqualTo("AUTO_RECOVERY");
        assertThat(solarAlert.getHealthyReadingsReceived()).isEqualTo(2);
        verify(events).save(argThat(e -> "ALERT_RESOLVED".equals(e.getEventType())));
    }
    @Test void warningAlertRequiresConfiguredConsecutiveAbnormalReadings() {
        Section section = Section.builder().id(30L).code("SEC-30").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();

        when(rules.lowVoltageEnabled()).thenReturn(true);
        when(rules.voltage("criticalKv", 1.5)).thenReturn(1.5);
        when(rules.voltage("warningKv", 3.0)).thenReturn(3.0);
        when(rules.abnormalReadingsRequired()).thenReturn(2);
        when(alerts.existsBySectionIdAndTypeAndStatusNot(30L, "LOW_VOLTAGE", "RESOLVED")).thenReturn(false);
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        // First abnormal reading (2.5 kV) should not trigger alert yet
        service.processTelemetry(section, fence, 2.5, 90);
        verify(alerts, never()).saveAndFlush(argThat(a -> "LOW_VOLTAGE".equals(a.getType()) && "WARNING".equals(a.getSeverity())));

        // Second consecutive abnormal reading (2.5 kV) triggers alert
        service.processTelemetry(section, fence, 2.5, 90);
        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "LOW_VOLTAGE".equals(a.getType()) && "WARNING".equals(a.getSeverity())));
    }
    @Test void normalReadingResetsAbnormalConsecutiveCounter() {
        Section section = Section.builder().id(31L).code("SEC-31").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();

        when(rules.lowVoltageEnabled()).thenReturn(true);
        when(rules.voltage("healthyKv", 5.0)).thenReturn(5.0);
        when(rules.voltage("criticalKv", 1.5)).thenReturn(1.5);
        when(rules.voltage("warningKv", 3.0)).thenReturn(3.0);
        when(rules.abnormalReadingsRequired()).thenReturn(2);
        when(alerts.existsBySectionIdAndTypeAndStatusNot(31L, "LOW_VOLTAGE", "RESOLVED")).thenReturn(false);

        // Reading 1: abnormal (2.5 kV) -> counter = 1
        service.processTelemetry(section, fence, 2.5, 90);
        verify(alerts, never()).saveAndFlush(argThat(a -> "LOW_VOLTAGE".equals(a.getType())));

        // Reading 2: healthy (5.5 kV) -> resets counter
        service.processTelemetry(section, fence, 5.5, 90);

        // Reading 3: abnormal again (2.5 kV) -> counter = 1, should not trigger because interrupted
        service.processTelemetry(section, fence, 2.5, 90);
        verify(alerts, never()).saveAndFlush(argThat(a -> "LOW_VOLTAGE".equals(a.getType())));
    }
    @Test void urgentRulesBypassAbnormalReadingsRequired() {
        Section section = Section.builder().id(32L).code("SEC-32").build();
        Fence fence = Fence.builder().id(20L).name("Test Fence").primaryMaintenanceUser(worker).build();

        when(rules.criticalVoltageEnabled()).thenReturn(true);
        when(rules.voltage("criticalKv", 1.5)).thenReturn(1.5);
        when(rules.abnormalReadingsRequired()).thenReturn(3); // Even if set to 3
        when(alerts.existsBySectionIdAndTypeAndStatusNot(32L, "LOW_VOLTAGE", "RESOLVED")).thenReturn(false);
        when(alerts.saveAndFlush(any(Alert.class))).thenAnswer(inv -> inv.getArgument(0));

        // Urgent critical voltage (1.0 kV) triggers immediately on reading 1
        service.processTelemetry(section, fence, 1.0, 90);
        verify(alerts, atLeastOnce()).saveAndFlush(argThat(a -> "LOW_VOLTAGE".equals(a.getType()) && "CRITICAL".equals(a.getSeverity())));
    }

    @Test void expiryDoesNotEscalateWhenEscalationDisabled() {
        alert.setFenceId(4L);
        alert.setAssigneeUserId(worker.getId());
        alert.setAssignmentStatus("AWAITING_ACCEPTANCE");
        alert.setAcceptanceDeadline(OffsetDateTime.now().minusSeconds(1));
        when(rules.enabled("escalationEnabled", true)).thenReturn(false);

        service.expireAssignment(1L);

        assertThat(alert.getAssigneeUserId()).isEqualTo(worker.getId());
        assertThat(alert.getAssignmentStatus()).isEqualTo("AWAITING_ACCEPTANCE");
    }

    @Test void publishIncludesBackupMaintenanceWhenNotifyMaintenanceEnabled() {
        alert.setFenceId(40L);
        User backup = User.builder().id(UUID.randomUUID()).fullName("Backup Worker").role(Role.MAINTENANCE).enabled(true).build();
        Fence fence = Fence.builder().id(40L).name("Fence 40").primaryMaintenanceUser(worker).backupMaintenanceUsers(Set.of(backup)).build();
        when(fences.findById(40L)).thenReturn(Optional.of(fence));
        when(users.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(users.findById(backup.getId())).thenReturn(Optional.of(backup));
        when(rules.notifications()).thenReturn(true);
        when(rules.enabled("notifyMaintenance", true)).thenReturn(true);
        when(rules.enabled("inAppEnabled", true)).thenReturn(true);

        service.reassignMaintenance(1L, new ReassignAlertRequestDTO(worker.getId(), "Reassigned"));

        verify(notifications, atLeastOnce()).sendNotification(argThat(n -> n.getUserId().equals(backup.getId())));
    }

    @Test void notifyUserDispatchesSmsWhenInAppDisabled() {
        alert.setFenceId(40L);
        Fence fence = Fence.builder().id(40L).name("Fence 40").primaryMaintenanceUser(worker).build();
        when(fences.findById(40L)).thenReturn(Optional.of(fence));
        when(users.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(rules.notifications()).thenReturn(true);
        when(rules.enabled("notifyMaintenance", true)).thenReturn(true);
        when(rules.inAppEnabled()).thenReturn(false);
        when(rules.smsEnabled()).thenReturn(true);

        service.reassignMaintenance(1L, new ReassignAlertRequestDTO(worker.getId(), "Reassigned"));

        verify(notifications, atLeastOnce()).sendNotification(argThat(n -> "SMS".equals(n.getChannels())));
    }

    @Test void notifyUserSuppressedWhenCategoryDisabled() {
        alert.setFenceId(40L);
        Fence fence = Fence.builder().id(40L).name("Fence 40").primaryMaintenanceUser(worker).build();
        when(fences.findById(40L)).thenReturn(Optional.of(fence));
        when(users.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(rules.notifications()).thenReturn(true);
        when(rules.enabled("notifyMaintenance", true)).thenReturn(true);
        when(rules.categoryEnabled("MAINTENANCE")).thenReturn(false);

        service.reassignMaintenance(1L, new ReassignAlertRequestDTO(worker.getId(), "Reassigned"));

        verify(notifications, never()).sendNotification(any());
    }

    @Test void maintenanceEventsTagNotificationAsMaintenance() {
        alert.setFenceId(40L);
        Fence fence = Fence.builder().id(40L).name("Fence 40").primaryMaintenanceUser(worker).build();
        when(fences.findById(40L)).thenReturn(Optional.of(fence));
        when(users.findById(worker.getId())).thenReturn(Optional.of(worker));
        when(rules.notifications()).thenReturn(true);
        when(rules.enabled("notifyMaintenance", true)).thenReturn(true);

        service.reassignMaintenance(1L, new ReassignAlertRequestDTO(worker.getId(), "Reassigned"));

        verify(notifications, atLeastOnce()).sendNotification(argThat(n -> "MAINTENANCE".equals(n.getCategory())));
    }
}

