package com.nerdc.elephantfence.backend.users.repository;

import com.nerdc.elephantfence.backend.users.entity.Role;
import com.nerdc.elephantfence.backend.users.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findByRole(Role role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.assignedProvinces LEFT JOIN FETCH u.assignedDistricts WHERE u.id = :id")
    Optional<User> findByIdWithProvincesAndDistricts(@Param("id") UUID id);
}
