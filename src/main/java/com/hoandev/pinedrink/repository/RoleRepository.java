package com.hoandev.pinedrink.repository;

import com.hoandev.pinedrink.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing {@link Role} entities.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, String> {

    /**
     * Finds a role by its unique code.
     *
     * @param code the role code to search for
     * @return an {@link Optional} containing the role if found
     */
    Optional<Role> findByCode(String code);
}
