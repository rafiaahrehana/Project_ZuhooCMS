package com.businessos.shared.address;

import java.util.List;

public interface LocationService {
    List<LocationDto> getCountries();
    List<LocationDto> getDivisionsForCountry(Long countryId);
    List<LocationDto> getChildren(Long parentId);
    LocationDto getNodeById(Long id);
    LocationDto createNode(CreateLocationRequest request);
    LocationDto updateNode(Long id, UpdateLocationRequest request);
    void deleteNode(Long id);
}
