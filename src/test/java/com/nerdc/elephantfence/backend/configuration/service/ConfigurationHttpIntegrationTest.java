package com.nerdc.elephantfence.backend.configuration.service;

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
class ConfigurationHttpIntegrationTest {
    @Autowired Environment environment;
    @Autowired UserRepository users;
    @Autowired PasswordEncoder passwords;

    @Test void configurationCanBeSavedAndLoadedOverHttp(){
        String email=UUID.randomUUID()+"@test.invalid";
        users.saveAndFlush(User.builder().email(email).fullName("Configuration test admin")
            .passwordHash(passwords.encode("Test-only-password-123!")).passwordChangeRequired(false).role(Role.SUPER_ADMIN).build());
        var client=RestClient.create("http://localhost:"+environment.getRequiredProperty("local.server.port"));
        var login=client.post().uri("/api/auth/login").body(Map.of("email",email,"password","Test-only-password-123!")).retrieve().body(Map.class);
        String bearer="Bearer "+login.get("accessToken");
        var loaded=client.get().uri("/api/configuration/general").header("Authorization",bearer).retrieve().body(Map.class);
        Map<String,Object> values=new HashMap<>((Map<String,Object>)loaded.get("value"));
        values.put("systemName","Persisted configuration test");
        var saved=client.put().uri("/api/configuration/general").header("Authorization",bearer)
            .body(Map.of("value",values,"reason","HTTP integration test")).retrieve().body(Map.class);
        assertThat(saved.get("updatedBy")).isEqualTo(email);
        var reloaded=client.get().uri("/api/configuration/general").header("Authorization",bearer).retrieve().body(Map.class);
        assertThat(((Map<?,?>)reloaded.get("value")).get("systemName")).isEqualTo("Persisted configuration test");
    }
}

