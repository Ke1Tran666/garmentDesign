package com.garmentDesign.service;

import com.garmentDesign.entity.Role;
import java.util.List;

public interface RoleService {
    List<Role> findAll();
    Role findById(Long id);
    Role save(Role data);
    Role update(Long id, Role data);
    void delete(Long id);
}
