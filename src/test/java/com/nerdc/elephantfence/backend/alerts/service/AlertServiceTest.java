package com.nerdc.elephantfence.backend.alerts.service;
import com.nerdc.elephantfence.backend.alerts.dto.*;
import com.nerdc.elephantfence.backend.alerts.entity.*;
import com.nerdc.elephantfence.backend.alerts.repository.*;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.sections.repository.SectionRepository;
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
}

