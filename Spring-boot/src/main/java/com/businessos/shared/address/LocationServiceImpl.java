package com.businessos.shared.address;

import com.businessos.shared.exception.BadRequestException;
import com.businessos.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class LocationServiceImpl implements LocationService {

    private final CountryRepository countryRepository;
    private final LocationRepository locationRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getCountries() {
        return countryRepository.findAll()
                .stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getDivisionsForCountry(Long countryId) {
        if (countryId == null) {
            throw new BadRequestException("Country ID is required to fetch divisions");
        }
        if (!countryRepository.existsById(countryId)) {
            throw new ResourceNotFoundException("Country not found with ID: " + countryId);
        }
        return locationRepository.findByCountryIdAndParentIsNull(countryId)
                .stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<LocationDto> getChildren(Long parentId) {
        if (parentId == null) {
            throw new BadRequestException("Parent ID is required to fetch children");
        }
        // Location-to-Location only (levels 2-4). Country -> LEVEL1 lookups go through
        // getDivisionsForCountry() instead - Country.id and Location.id are independent
        // auto-increment sequences that can collide, so probing "does a Location with
        // this id exist" here would silently resolve to the wrong table for some countries.
        if (!locationRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Parent node not found with ID: " + parentId);
        }
        return locationRepository.findByParentId(parentId)
                .stream()
                .map(LocationMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public LocationDto getNodeById(Long id) {
        return locationRepository.findById(id)
                .map(LocationMapper::toDto)
                .or(() -> countryRepository.findById(id).map(LocationMapper::toDto))
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));
    }

    @Override
    public LocationDto createNode(CreateLocationRequest request) {
        Country country;
        Location parent = null;

        if (request.getType() == LocationType.LEVEL1) {
            if (request.getCountryId() == null) {
                throw new BadRequestException("LEVEL1 nodes require a countryId");
            }
            country = countryRepository.findById(request.getCountryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Country not found with ID: " + request.getCountryId()));
        } else {
            if (request.getParentId() == null) {
                throw new BadRequestException("Nodes of type " + request.getType() + " require a parentId");
            }
            parent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent node not found with ID: " + request.getParentId()));
            validateParentType(request.getType(), parent.getType());
            country = parent.getCountry();
        }

        Location node = Location.builder()
                .name(request.getName().trim())
                .type(request.getType())
                .country(country)
                .parent(parent)
                .build();

        return LocationMapper.toDto(locationRepository.save(node));
    }

    @Override
    public LocationDto updateNode(Long id, UpdateLocationRequest request) {
        Location node = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));

        if (request.getName() != null) {
            node.setName(request.getName().trim());
        }

        if (request.getParentId() != null) {
            if (id.equals(request.getParentId())) {
                throw new BadRequestException("A node cannot be its own parent");
            }
            Location newParent = locationRepository.findById(request.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent node not found with ID: " + request.getParentId()));
            validateParentType(node.getType(), newParent.getType());
            node.setParent(newParent);
            node.setCountry(newParent.getCountry());
        }

        return LocationMapper.toDto(locationRepository.save(node));
    }

    @Override
    public void deleteNode(Long id) {
        Location node = locationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Node not found with ID: " + id));
        locationRepository.delete(node);
    }

    private void validateParentType(LocationType type, LocationType parentType) {
        LocationType expectedParentType;
        switch (type) {
            case LEVEL2:
                expectedParentType = LocationType.LEVEL1;
                break;
            case LEVEL3:
                expectedParentType = LocationType.LEVEL2;
                break;
            case LEVEL4:
                expectedParentType = LocationType.LEVEL3;
                break;
            default:
                throw new BadRequestException("Invalid node type for a child node: " + type);
        }
        if (parentType != expectedParentType) {
            throw new BadRequestException("Parent node type must be " + expectedParentType + " for child of type " + type + ", but was " + parentType);
        }
    }
}
