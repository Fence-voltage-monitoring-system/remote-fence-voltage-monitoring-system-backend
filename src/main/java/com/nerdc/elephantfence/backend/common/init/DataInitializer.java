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
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedProvincesAndDistricts();
        seedSuperAdmin();
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
        if (userRepository.count() > 0) {
            return;
        }

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
        log.info("Default Super Admin created successfully.");
    }
}
