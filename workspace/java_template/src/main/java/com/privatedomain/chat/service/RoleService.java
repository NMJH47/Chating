package com.privatedomain.chat.service;

import com.privatedomain.chat.model.Role;
import java.util.Optional;

/**
 * Service interface for managing role operations
 */
public interface RoleService {
    
    /**
     * Find a role by its name
     * 
     * @param name the role name to search for
     * @return an Optional containing the role if found
     */
    Optional<Role> findByName(String name);
    
    /**
     * Check if a role exists by name
     * 
     * @param name the role name to check
     * @return true if the role exists, false otherwise
     */
    boolean existsByName(String name);
    
    /**
     * Create a new role if it doesn't exist
     * 
     * @param role the role to create
     * @return the created or existing role
     */
    Role createRoleIfNotExists(Role role);
    
    /**
     * Find a role by its ID
     * 
     * @param id the role ID to search for
     * @return an Optional containing the role if found
     */
    Optional<Role> findById(Integer id);
    
    /**
     * Get all available roles
     * 
     * @return a list of all roles
     */
    java.util.List<Role> findAll();
}