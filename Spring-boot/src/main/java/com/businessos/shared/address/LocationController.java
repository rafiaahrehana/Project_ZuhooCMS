package com.businessos.shared.address;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Locations", description = "Country and location-hierarchy management")
public class LocationController {

    private final LocationService locationService;

    @GetMapping("/countries")
    @Operation(summary = "Get all countries")
    public ResponseEntity<List<LocationDto>> getCountries() {
        return ResponseEntity.ok(locationService.getCountries());
    }

    @GetMapping("/countries/{countryId}/divisions")
    @Operation(summary = "Get a country's top-level (LEVEL1) divisions")
    public ResponseEntity<List<LocationDto>> getDivisionsForCountry(@PathVariable Long countryId) {
        return ResponseEntity.ok(locationService.getDivisionsForCountry(countryId));
    }

    @GetMapping("/children/{parentId}")
    @Operation(summary = "Get child nodes by parent Location ID (levels 2-4 only - use /countries/{id}/divisions for LEVEL1)")
    public ResponseEntity<List<LocationDto>> getChildren(@PathVariable Long parentId) {
        return ResponseEntity.ok(locationService.getChildren(parentId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a node by ID")
    public ResponseEntity<LocationDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(locationService.getNodeById(id));
    }

    // Mutating endpoints require platform-admin auth even though GETs on this
    // path are public (see SecurityConfig - only GET /api/locations/** is
    // permitAll, these three are not covered by that rule).
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PostMapping
    @Operation(summary = "Create a new location node")
    public ResponseEntity<LocationDto> create(@Valid @RequestBody CreateLocationRequest request) {
        return new ResponseEntity<>(locationService.createNode(request), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @PutMapping("/{id}")
    @Operation(summary = "Update an existing location node")
    public ResponseEntity<LocationDto> update(@PathVariable Long id, @Valid @RequestBody UpdateLocationRequest request) {
        return ResponseEntity.ok(locationService.updateNode(id, request));
    }

    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'SYSTEM_ADMIN')")
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a location node")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        locationService.deleteNode(id);
        return ResponseEntity.noContent().build();
    }
}
