package com.hotel.channelmanager.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPropertyAccessRepository extends JpaRepository<UserPropertyAccess, String> {
    List<UserPropertyAccess> findByUserId(String userId);
    List<UserPropertyAccess> findByPropertyId(String propertyId);
    Optional<UserPropertyAccess> findByUserIdAndPropertyId(String userId, String propertyId);
    void deleteByUserIdAndPropertyId(String userId, String propertyId);
}

