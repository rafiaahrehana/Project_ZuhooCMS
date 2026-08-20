package com.businessos.shared.address;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {

    private Long id;
    private String country;
    private String level1;
    private String level2;
    private String level3;
    private String level4;
    private String streetAddress;
    private String postalCode;
    private String apartment;
}
