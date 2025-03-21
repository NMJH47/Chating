package com.privatedomain.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.privatedomain.chat.mapper.RoleMapper;
import com.privatedomain.chat.model.Role;
import com.privatedomain.chat.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Implementation of RoleService using MyBatis Plus
 */
@Service
@RequiredArgsConstructor
public class RoleServiceImpl extends ServiceImpl<RoleMapper, Role> implements RoleService {

    private final RoleMapper roleMapper;
    
    @Override
    public Optional<Role> findByName(String name) {
        Role role = roleMapper.findByName(name);
        return Optional.ofNullable(role);
    }
    
    @Override
    public boolean existsByName(String name) {
        return roleMapper.existsByName(name);
    }
    
    @Override
    public Role createRoleIfNotExists(Role role) {
        String roleName = role.getName().toString();
        
        // Check if role exists
        Role existingRole = roleMapper.findByName(roleName);
        if (existingRole != null) {
            return existingRole;
        }
        
        // Create new role
        save(role);
        return role;
    }
    
    @Override
    public Optional<Role> findById(Integer id) {
        return Optional.ofNullable(getById(id));
    }
    
    @Override
    public List<Role> findAll() {
        return list();
    }
}