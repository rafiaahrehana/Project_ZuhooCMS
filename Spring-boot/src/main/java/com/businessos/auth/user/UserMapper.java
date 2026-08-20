package com.businessos.auth.user;

public  class UserMapper {


    public static UserResponse toResponse(User user) {
        UserResponse r = new UserResponse();
        r.setId(user.getId());
        r.setFirstName(user.getFirstName());
        r.setLastName(user.getLastName());
        r.setEmail(user.getEmail());
        r.setPhone(user.getPhone());
        r.setImage(user.getImage());
        r.setRole(user.getRole());
        r.setActive(user.isActive());
        r.setEmailVerified(user.isEmailVerified());
        r.setLanguagePreference(user.getLanguagePreference());
        r.setCreatedAt(user.getCreatedAt());
        return r;
    }
}
