package com.businessos.auth.user;

import com.businessos.auth.role.enums.Role;
import com.businessos.shared.address.AddressResponse;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String image;
    private Role role;
    private String languagePreference;
    private AddressResponse location;
    private String companyEmail;
}
