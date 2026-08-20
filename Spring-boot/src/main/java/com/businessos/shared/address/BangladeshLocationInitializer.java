package com.businessos.shared.address;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Order(2)
@Slf4j
public class BangladeshLocationInitializer implements CommandLineRunner {

    private final CountryRepository countryRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional
    public void run(String... args) {
        Optional<Country> bdOpt = countryRepository.findByCode("BD");
        if (bdOpt.isEmpty()) {
            log.warn("Bangladesh (BD) country master record not found. Skipping location seeding.");
            return;
        }

        Country bd = bdOpt.get();

        // Check if locations for BD are already seeded
        long existingCount = locationRepository.countByCountryId(bd.getId());
        if (existingCount > 0) {
            log.info("Bangladesh locations are already initialized ({} nodes found).", existingCount);
            return;
        }

        log.info("Starting local seeding for Bangladesh administrative levels (Divisions, Districts, Upazilas/Thanas) from local JSON resources...");

        try {
            // Read divisions, districts, upazilas from local resources
            String divisionsJson = readResource("data/bd-divisions.json");
            String districtsJson = readResource("data/bd-districts.json");
            String upazilasJson = readResource("data/bd-upazilas.json");

            ObjectMapper mapper = new ObjectMapper();

            DivisionsWrapper divisionsWrapper = mapper.readValue(divisionsJson, DivisionsWrapper.class);
            DistrictsWrapper districtsWrapper = mapper.readValue(districtsJson, DistrictsWrapper.class);
            UpazilasWrapper upazilasWrapper = mapper.readValue(upazilasJson, UpazilasWrapper.class);

            if (divisionsWrapper.divisions == null || districtsWrapper.districts == null || upazilasWrapper.upazilas == null) {
                log.warn("Failed to parse local location datasets. Seeding skipped.");
                return;
            }

            // Group districts by division_id for O(1) matching
            Map<String, List<DistrictRaw>> districtsByDivision = districtsWrapper.districts.stream()
                    .collect(Collectors.groupingBy(dist -> String.valueOf(dist.division_id)));

            // Group upazilas by district_id for O(1) matching
            Map<String, List<UpazilaRaw>> upazilasByDistrict = upazilasWrapper.upazilas.stream()
                    .collect(Collectors.groupingBy(up -> String.valueOf(up.district_id)));

            int divisionCount = 0;
            int districtCount = 0;
            int upazilaCount = 0;

            for (DivisionRaw div : divisionsWrapper.divisions) {
                // Save division (LEVEL 1)
                Location divisionNode = saveNode(div.name.trim(), LocationType.LEVEL1, bd, null);
                divisionCount++;

                List<DistrictRaw> districts = districtsByDivision.getOrDefault(String.valueOf(div.id), List.of());
                for (DistrictRaw dist : districts) {
                    // Save district (LEVEL 2)
                    Location districtNode = saveNode(dist.name.trim(), LocationType.LEVEL2, bd, divisionNode);
                    districtCount++;

                    List<UpazilaRaw> upazilas = upazilasByDistrict.getOrDefault(String.valueOf(dist.id), List.of());
                    for (UpazilaRaw up : upazilas) {
                        // Save upazila/thana (LEVEL 3)
                        saveNode(up.name.trim(), LocationType.LEVEL3, bd, districtNode);
                        upazilaCount++;
                    }
                }
            }

            log.info("Successfully seeded Bangladesh locations: {} Divisions, {} Districts, {} Upazilas/Thanas.",
                    divisionCount, districtCount, upazilaCount);

        } catch (Exception e) {
            log.error("Failed to seed Bangladesh locations from local files", e);
        }
    }

    private String readResource(String path) throws Exception {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream inputStream = resource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private Location saveNode(String name, LocationType type, Country country, Location parent) {
        Location node = Location.builder()
                .name(name)
                .type(type)
                .country(country)
                .parent(parent)
                .build();
        return locationRepository.save(node);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DivisionsWrapper {
        public List<DivisionRaw> divisions;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DivisionRaw {
        public String id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DistrictsWrapper {
        public List<DistrictRaw> districts;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DistrictRaw {
        public String id;
        public String division_id;
        public String name;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpazilasWrapper {
        public List<UpazilaRaw> upazilas;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class UpazilaRaw {
        public String id;
        public String district_id;
        public String name;
    }
}
