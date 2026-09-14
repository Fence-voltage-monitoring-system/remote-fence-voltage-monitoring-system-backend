package com.nerdc.elephantfence.backend.alerts.service;

import com.nerdc.elephantfence.backend.alerts.dto.CreateAlertRequestDTO;
import com.nerdc.elephantfence.backend.alerts.repository.AlertRepository;
import com.nerdc.elephantfence.backend.common.security.UserPrincipal;
import com.nerdc.elephantfence.backend.fences.entity.Fence;
import com.nerdc.elephantfence.backend.fences.repository.FenceRepository;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.users.entity.*;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import java.math.BigDecimal;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AlertRegistrationIntegrationTest {
    @Autowired AlertService service;
    @Autowired UserRepository users;
    @Autowired FenceRepository fences;
    @Autowired DistrictRepository districts;
    @Autowired AlertRepository alerts;
    @Autowired PlatformTransactionManager transactions;
    @AfterEach void clear(){SecurityContextHolder.clearContext();}
    @Test void registersIncidentAndCommitsNotification(){
        var tx=new TransactionTemplate(transactions);
        Long fenceId=tx.execute(status->{
            var district=districts.findAll().getFirst();
            var staff=users.save(User.builder().fullName("Test maintenance").email(UUID.randomUUID()+"@test.invalid").passwordHash("unused-test-hash").role(Role.MAINTENANCE).build());
            return fences.saveAndFlush(Fence.builder().code("TEST-"+UUID.randomUUID()).name("Registration test fence").province(district.getProvince()).district(district).primaryMaintenanceUser(staff).build()).getId();
        });
        var admin=users.findByRole(Role.SUPER_ADMIN).getFirst();
        var principal=UserPrincipal.create(admin);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal,null,principal.getAuthorities()));
        var request=new CreateAlertRequestDTO();
        request.setFenceId(fenceId);request.setTitle("Test wire break");request.setType("WIRE_BREAK");request.setSeverity("WARNING");request.setDescription("Registration integration test");
        request.setDetectedVoltageKv(BigDecimal.ZERO);request.setThresholdVoltageKv(new BigDecimal("4.5"));
        var saved=service.create(request);
        assertThat(saved.getId()).isNotNull();
        assertThat(alerts.findById(saved.getId())).isPresent();
        assertThat(saved.getTimeline()).isNotEmpty();
        assertThat(saved.getAssignmentStatus()).isEqualTo("AWAITING_ACCEPTANCE");
    }
}

