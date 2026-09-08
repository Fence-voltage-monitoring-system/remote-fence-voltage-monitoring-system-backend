package com.nerdc.elephantfence.backend.common.init;

import com.nerdc.elephantfence.backend.locations.entity.District;
import com.nerdc.elephantfence.backend.locations.entity.Province;
import com.nerdc.elephantfence.backend.locations.repository.DistrictRepository;
import com.nerdc.elephantfence.backend.locations.repository.ProvinceRepository;
import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import com.nerdc.elephantfence.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final com.nerdc.elephantfence.backend.devices.repository.DeviceRepository deviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedProvincesAndDistricts();
        seedSuperAdmin();
        seedFencesSectionsAndDevices();
    }

    private void seedProvincesAndDistricts() {
        if (provinceRepository.count() > 0) {
            return;
        }

        log.info("Seeding initial Sri Lanka Provinces and Districts...");

        Map<String, List<String>> provinceDistrictMap = Map.of(
                "Western", List.of("Colombo", "Gampaha", "Kalutara"),
                "Central", List.of("Kandy", "Matale", "Nuwara Eliya"),
                "Southern", List.of("Galle", "Matara", "Hambantota"),
                "North Western", List.of("Kurunegala", "Puttalam"),
                "North Central", List.of("Anuradhapura", "Polonnaruwa"),
                "Uva", List.of("Badulla", "Monaragala"),
                "Sabaragamuwa", List.of("Ratnapura", "Kegalle"),
                "Eastern", List.of("Trincomalee", "Batticaloa", "Ampara"),
                "Northern", List.of("Jaffna", "Kilinochchi", "Mannar", "Vavuniya", "Mullaitivu")
        );

        provinceDistrictMap.forEach((provName, districtList) -> {
            Province province = provinceRepository.save(Province.builder().name(provName).build());
            for (String distName : districtList) {
                districtRepository.save(District.builder().province(province).name(distName).build());
            }
        });

        log.info("Finished seeding Provinces and Districts.");
    }

    private void seedSuperAdmin() {
        userRepository.findByEmailIgnoreCase("kasun.perera@dwc.gov.lk").ifPresent(u -> {
            log.info("Deleting legacy demo user kasun.perera@dwc.gov.lk");
            userRepository.delete(u);
        });
        userRepository.findByEmailIgnoreCase("june.kartha@dwc.gov.lk").ifPresent(u -> {
            log.info("Deleting legacy demo user june.kartha@dwc.gov.lk");
            userRepository.delete(u);
        });

        if (!userRepository.existsByEmailIgnoreCase("admin@nerdc.lk")) {
            log.info("Seeding default Super Admin user (admin@nerdc.lk)...");
            User admin = User.builder()
                    .fullName("System Administrator")
                    .email("admin@nerdc.lk")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.SUPER_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("ADM-001")
                    .contactNumber("+94112223344")
                    .build();
            userRepository.save(admin);
        }
    }

    private void seedFencesSectionsAndDevices() {
        Integer fenceCount = jdbcTemplate.queryForObject("SELECT count(*) FROM fences", Integer.class);
        if (fenceCount != null && fenceCount > 0) {
            return;
        }

        log.info("Seeding initial Fences and Sections via SQL...");

        Long monId = jdbcTemplate.queryForObject("SELECT id FROM districts WHERE LOWER(name) = 'monaragala' LIMIT 1", Long.class);
        Long monProvId = jdbcTemplate.queryForObject("SELECT province_id FROM districts WHERE LOWER(name) = 'monaragala' LIMIT 1", Long.class);

        Long putId = jdbcTemplate.queryForObject("SELECT id FROM districts WHERE LOWER(name) = 'puttalam' LIMIT 1", Long.class);
        Long putProvId = jdbcTemplate.queryForObject("SELECT province_id FROM districts WHERE LOWER(name) = 'puttalam' LIMIT 1", Long.class);

        Long anuId = jdbcTemplate.queryForObject("SELECT id FROM districts WHERE LOWER(name) = 'anuradhapura' LIMIT 1", Long.class);
        Long anuProvId = jdbcTemplate.queryForObject("SELECT province_id FROM districts WHERE LOWER(name) = 'anuradhapura' LIMIT 1", Long.class);

        Long ampId = jdbcTemplate.queryForObject("SELECT id FROM districts WHERE LOWER(name) = 'ampara' LIMIT 1", Long.class);
        Long ampProvId = jdbcTemplate.queryForObject("SELECT province_id FROM districts WHERE LOWER(name) = 'ampara' LIMIT 1", Long.class);

        if (monId != null && putId != null && anuId != null && ampId != null) {
            jdbcTemplate.execute("DELETE FROM sections");
            jdbcTemplate.execute("DELETE FROM fences");

            jdbcTemplate.update("INSERT INTO fences (code, name, province_id, district_id, length_km, average_voltage_kv, health) VALUES ('EPF-MON-01', 'Monaragala Elephant Protection Fence', ?, ?, 14.5, 6.2, 'HEALTHY')", monProvId, monId);
            jdbcTemplate.update("INSERT INTO fences (code, name, province_id, district_id, length_km, average_voltage_kv, health) VALUES ('EPF-WIL-01', 'Wilpattu North Buffer Fence', ?, ?, 11.2, 5.8, 'HEALTHY')", putProvId, putId);
            jdbcTemplate.update("INSERT INTO fences (code, name, province_id, district_id, length_km, average_voltage_kv, health) VALUES ('EPF-MIH-01', 'Mihintale Wildlife Buffer Fence', ?, ?, 9.8, 3.2, 'WARNING')", anuProvId, anuId);
            jdbcTemplate.update("INSERT INTO fences (code, name, province_id, district_id, length_km, average_voltage_kv, health) VALUES ('EPF-GAL-01', 'Gal Oya East Protection Fence', ?, ?, 13.4, 3.8, 'WARNING')", ampProvId, ampId);

            Long f1 = jdbcTemplate.queryForObject("SELECT id FROM fences WHERE code = 'EPF-MON-01'", Long.class);
            Long f2 = jdbcTemplate.queryForObject("SELECT id FROM fences WHERE code = 'EPF-WIL-01'", Long.class);
            Long f3 = jdbcTemplate.queryForObject("SELECT id FROM fences WHERE code = 'EPF-MIH-01'", Long.class);
            Long f4 = jdbcTemplate.queryForObject("SELECT id FROM fences WHERE code = 'EPF-GAL-01'", Long.class);

            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-001', 3.5, 'HEALTHY')", f1);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-002', 3.8, 'WARNING')", f1);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-003', 3.6, 'HEALTHY')", f1);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-004', 3.6, 'HEALTHY')", f1);

            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-001', 3.8, 'HEALTHY')", f2);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-002', 3.7, 'HEALTHY')", f2);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-003', 3.7, 'HEALTHY')", f2);

            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-001', 3.2, 'HEALTHY')", f3);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-002', 3.3, 'OFFLINE')", f3);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-003', 3.3, 'OFFLINE')", f3);

            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-001', 3.4, 'WARNING')", f4);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-002', 3.3, 'HEALTHY')", f4);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-003', 3.3, 'HEALTHY')", f4);
            jdbcTemplate.update("INSERT INTO sections (fence_id, code, length_km, status) VALUES (?, 'SEC-004', 3.4, 'HEALTHY')", f4);
        }
        log.info("Finished seeding initial Fences and Sections.");
    }
}
