package com.businessos.auth.user;

import com.businessos.shared.address.AddressRequest;
import lombok.Data;

@Data
public class UserProfileRequest {
    private String firstName;
    private String lastName;
    private String email;
    // Required whenever email is being changed - verified against the account's
    // actual password before the change is applied.
    private String currentPassword;
    private String phone;
    private String image;
    private String languagePreference;
    private AddressRequest location;
    private String companyEmail;
}
