package com.privatedomain.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.privatedomain.chat.model.Role;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Optional;

/**
 * MyBatis Plus mapper interface for Role entity
 * Provides CRUD operations and custom queries
 */
@Mapper
public interface RoleMapper extends BaseMapper<Role> {
    
    /**
     * Find a role by its name
     *
     * @param name the role name to find
     * @return the matching role
     */
    @Select("SELECT * FROM roles WHERE name = #{name}")
    Role findByName(@Param("name") String name);
    
    /**
     * Check if a role exists by name
     *
     * @param name the role name to check
     * @return true if role exists, false otherwise
     */
    @Select("SELECT COUNT(*) FROM roles WHERE name = #{name}")
    boolean existsByName(@Param("name") String name);
}