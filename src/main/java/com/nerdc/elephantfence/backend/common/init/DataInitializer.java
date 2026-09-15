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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final ProvinceRepository provinceRepository;
    private final DistrictRepository districtRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        seedProvincesAndDistricts();
        seedSuperAdmin();
        seedOperationalUsers();
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

        if (!userRepository.existsByEmailIgnoreCase("kasun.perera@dwc.gov.lk")) {
            userRepository.save(User.builder()
                    .fullName("Kasun Perera")
                    .email("kasun.perera@dwc.gov.lk")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.SUPER_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("ADM-002")
                    .contactNumber("+94112223355")
                    .build());
        }

        Province western = provinceRepository.findAll().stream().filter(p -> "Western".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
        Province uva = provinceRepository.findAll().stream().filter(p -> "Uva".equalsIgnoreCase(p.getName())).findFirst().orElse(null);
        District colombo = districtRepository.findAll().stream().filter(d -> "Colombo".equalsIgnoreCase(d.getName())).findFirst().orElse(null);
        District monaragala = districtRepository.findAll().stream().filter(d -> "Monaragala".equalsIgnoreCase(d.getName())).findFirst().orElse(null);

        if (western != null && !userRepository.existsByEmailIgnoreCase("june.kartha@dwc.gov.lk")) {
            userRepository.save(User.builder()
                    .fullName("June Kartha")
                    .email("june.kartha@dwc.gov.lk")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.REGIONAL_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("RA-WST-00")
                    .assignedProvinces(Set.of(western))
                    .build());
        }

        if (uva != null && monaragala != null && !userRepository.existsByEmailIgnoreCase("kamal.silva@dwc.gov.lk")) {
            userRepository.save(User.builder()
                    .fullName("Kamal Silva")
                    .email("kamal.silva@dwc.gov.lk")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.FIELD_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("FA-MON-00")
                    .assignedProvinces(Set.of(uva))
                    .assignedDistricts(Set.of(monaragala))
                    .build());
        }

        if (western != null && colombo != null && !userRepository.existsByEmailIgnoreCase("duruthu@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("Duruthu Charulatha")
                    .email("duruthu@gmail.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.FIELD_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("FA-COL-00")
                    .assignedProvinces(Set.of(western))
                    .assignedDistricts(Set.of(colombo))
                    .build());
        }

        if (western != null && colombo != null && !userRepository.existsByEmailIgnoreCase("ridge@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("ridge")
                    .email("ridge@gmail.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.FIELD_ADMIN)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("FA-COL-99")
                    .assignedProvinces(Set.of(western))
                    .assignedDistricts(Set.of(colombo))
                    .build());
        }

        if (uva != null && monaragala != null && !userRepository.existsByEmailIgnoreCase("morcha@gmail.com")) {
            userRepository.save(User.builder()
                    .fullName("morcha")
                    .email("morcha@gmail.com")
                    .passwordHash(passwordEncoder.encode("Admin@123456"))
                    .role(Role.MAINTENANCE)
                    .enabled(true)
                    .passwordChangeRequired(false)
                    .staffId("MN-MON-00")
                    .assignedProvinces(Set.of(uva))
                    .assignedDistricts(Set.of(monaragala))
                    .build());
        }
    }

    private void seedOperationalUsers() {
        if (userRepository.count() > 10) {
            return;
        }

        log.info("Seeding operational workforce users (Regional Admins, Field Admins, Maintenance Staff)...");
        String defaultPasswordHash = passwordEncoder.encode("Dwc@123456");

        List<Province> provinces = provinceRepository.findAll();
        for (Province province : provinces) {
            String pSlug = toSlug(province.getName());
            String pCode = toCode(province.getName());

            // 1 Regional Admin per Province
            String raEmail = "ra." + pSlug + "@dwc.gov.lk";
            if (!userRepository.existsByEmailIgnoreCase(raEmail)) {
                User ra = User.builder()
                        .fullName(province.getName() + " Regional Admin")
                        .email(raEmail)
                        .passwordHash(defaultPasswordHash)
                        .role(Role.REGIONAL_ADMIN)
                        .enabled(true)
                        .passwordChangeRequired(false)
                        .staffId("RA-" + pCode + "-01")
                        .contactNumber("+94" + (11 + province.getId()) + "220000")
                        .assignedProvinces(Set.of(province))
                        .build();
                userRepository.save(ra);
            }

            // 2 Field Admins & 2 Maintenance Staff per District
            List<District> districts = districtRepository.findByProvinceIdOrderByNameAsc(province.getId());
            for (District district : districts) {
                String dSlug = toSlug(district.getName());
                String dCode = toCode(district.getName());

                for (int i = 1; i <= 2; i++) {
                    // Field Admin
                    String faEmail = "fa" + i + "." + dSlug + "@dwc.gov.lk";
                    if (!userRepository.existsByEmailIgnoreCase(faEmail)) {
                        User fa = User.builder()
                                .fullName(district.getName() + " Field Admin " + i)
                                .email(faEmail)
                                .passwordHash(defaultPasswordHash)
                                .role(Role.FIELD_ADMIN)
                                .enabled(true)
                                .passwordChangeRequired(false)
                                .staffId("FA-" + dCode + "-0" + i)
                                .contactNumber("+9477" + String.format("%07d", (district.getId() * 100 + i * 10)))
                                .assignedProvinces(Set.of(province))
                                .assignedDistricts(Set.of(district))
                                .build();
                        userRepository.save(fa);
                    }

                    // Maintenance Staff
                    String maintEmail = "maint" + i + "." + dSlug + "@dwc.gov.lk";
                    if (!userRepository.existsByEmailIgnoreCase(maintEmail)) {
                        User maint = User.builder()
                                .fullName(district.getName() + " Maintenance " + i)
                                .email(maintEmail)
                                .passwordHash(defaultPasswordHash)
                                .role(Role.MAINTENANCE)
                                .enabled(true)
                                .passwordChangeRequired(false)
                                .staffId("MN-" + dCode + "-0" + i)
                                .contactNumber("+9471" + String.format("%07d", (district.getId() * 100 + i * 10)))
                                .assignedProvinces(Set.of(province))
                                .assignedDistricts(Set.of(district))
                                .build();
                        userRepository.save(maint);
                    }
                }
            }
        }
        log.info("Finished seeding operational workforce users.");
    }

    private String toSlug(String name) {
        return name.toLowerCase().replaceAll("[^a-z0-9]", "");
    }

    private String toCode(String name) {
        String clean = name.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return clean.length() >= 3 ? clean.substring(0, 3) : clean;
    }
}
