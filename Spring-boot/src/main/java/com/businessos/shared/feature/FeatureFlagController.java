package com.businessos.shared.feature;

import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feature-flags")
public class FeatureFlagController {

    private static final Map<String, String> DEFAULT_FLAGS = Map.of(
        "ENABLE_BKASH",       "Show bKash payment option to clients",
        "ENABLE_NAGAD",       "Show Nagad payment option to clients",
        "ENABLE_REFERRAL",    "Enable the company referral programme",
        "ENABLE_AUTO_ASSIGN", "Auto-assign servicereview requests to staff by workload",
        "ENABLE_PACKAGES",    "Show bundled servicereview packages in catalog",
        "MAINTENANCE_MODE",   "Block all requests — show maintenance page to users"
    );

    private final FeatureFlagRepository flagRepository;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<FeatureFlag>> getAll() {
        return ResponseEntity.ok(flagRepository.findAll());
    }

    @GetMapping("/{key}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FeatureFlag> getByKey(@PathVariable String key) {
        return ResponseEntity.ok(flagRepository.findByFlagKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag not found: " + key)));
    }

    // These are platform-wide flags (including MAINTENANCE_MODE, which blocks all
    // requests) - only platform admins should be able to flip them.
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PatchMapping("/{key}/toggle")
    @Transactional
    public ResponseEntity<FeatureFlag> toggle(@PathVariable String key) {
        FeatureFlag flag = flagRepository.findByFlagKey(key)
                .orElseThrow(() -> new ResourceNotFoundException("Feature flag not found: " + key));
        flag.setEnabled(!flag.isEnabled());
        flagRepository.save(flag);
        return ResponseEntity.ok(flag);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping("/seed")
    @Transactional
    public ResponseEntity<String> seed() {
        DEFAULT_FLAGS.forEach((key, description) -> {
            if (!flagRepository.existsByFlagKey(key)) {
                flagRepository.save(FeatureFlag.builder()
                        .flagKey(key)
                        .enabled(true)
                        .description(description)
                        .build());
            }
        });
        return ResponseEntity.ok("Default feature flags seeded successfully");
    }
}
