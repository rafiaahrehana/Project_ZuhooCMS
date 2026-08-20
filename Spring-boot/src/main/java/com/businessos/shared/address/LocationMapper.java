package com.businessos.shared.address;

public final class LocationMapper {

    private LocationMapper() {}

    public static LocationDto toDto(Country country) {
        if (country == null) {
            return null;
        }
        return new LocationDto(country.getId(), country.getName(), "COUNTRY", country.getCode());
    }

    public static LocationDto toDto(Location node) {
        if (node == null) {
            return null;
        }
        return new LocationDto(
            node.getId(),
            node.getName(),
            node.getType() != null ? node.getType().name() : null,
            null
        );
    }
}
