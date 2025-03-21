package com.privatedomain.chat.repository.postgres;

import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.model.Role.ERole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing roles in the database.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Integer> {
    
    /**
     * Find a role by its name.
     *
     * @param name Role name
     * @return Optional containing the role if found
     */
    Optional<Role> findByName(ERole name);
}