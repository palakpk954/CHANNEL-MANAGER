package com.hotel.channelmanager.identity.web.dto;

import com.hotel.channelmanager.identity.domain.Role;
import com.hotel.channelmanager.identity.domain.User;

public record UserDto(
        String id,
        String organizationId,
        String email,
        String firstName,
        String lastName,
        Role role,
        String status
) {
    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getOrganizationId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole(),
                user.getStatus()
        );
    }
}

