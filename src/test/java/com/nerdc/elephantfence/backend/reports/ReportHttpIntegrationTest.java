package com.nerdc.elephantfence.backend.reports;

import com.nerdc.elephantfence.backend.users.entity.*;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestClient;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ReportHttpIntegrationTest {
    @Autowired Environment environment;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;
    @Autowired com.nerdc.elephantfence.backend.fences.repository.FenceRepository fences;
    @Autowired com.nerdc.elephantfence.backend.locations.repository.DistrictRepository districts;

    @Test
    @SuppressWarnings("unchecked")
    void reportFiltersCanBeLoadedOverHttp(){
        String email=UUID.randomUUID()+"@test.invalid";
        users.saveAndFlush(User.builder().email(email).fullName("Configuration test admin")
            .passwordHash(passwords.encode("Test-only-password-123!")).passwordChangeRequired(false).role(Role.SUPER_ADMIN).build());
        var client=RestClient.create("http://localhost:"+environment.getRequiredProperty("local.server.port"));
        var login=client.post().uri("/api/auth/login").body(Map.of("email",email,"password","Test-only-password-123!")).retrieve().body(Map.class);
        String bearer="Bearer "+login.get("accessToken");
        var district=districts.findAll().getFirst();
        var fence=fences.saveAndFlush(com.nerdc.elephantfence.backend.fences.entity.Fence.builder()
            .code("HTTP-"+UUID.randomUUID()).name("Registered test fence").province(district.getProvince()).district(district).build());
        var loaded=client.get().uri("/api/reports/filters").header("Authorization",bearer).retrieve().body(Map.class);
        assertThat((java.util.List<Map<String,Object>>)loaded.get("fences"))
            .anySatisfy(option->assertThat(option.get("value")).isEqualTo(fence.getCode()));
        assertThat(client.get().uri("/api/reports/filters?fence=NONEXISTENT")
            .header("Authorization",bearer).<Integer>exchange((request,response)->response.getStatusCode().value())).isEqualTo(403);
        assertThat(client.get().uri("/api/reports/missing-endpoint")
            .header("Authorization",bearer).<Integer>exchange((request,response)->response.getStatusCode().value())).isEqualTo(404);
    }
    @Test void invalidSessionsReturnUnauthorizedRatherThanScopeDenied() {
        var client=RestClient.create("http://localhost:"+environment.getRequiredProperty("local.server.port"));
        assertThat(client.get().uri("/api/reports/filters")
            .<Integer>exchange((request,response)->response.getStatusCode().value())).isEqualTo(401);
        assertThat(client.get().uri("/api/reports/filters").header("Authorization","Bearer invalid")
            .<Integer>exchange((request,response)->response.getStatusCode().value())).isEqualTo(401);
    }
}

