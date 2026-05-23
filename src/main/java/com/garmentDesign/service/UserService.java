package com.garmentDesign.service;

import com.garmentDesign.entity.User;
import java.util.List;

public interface UserService {
    List<User> findAll();
    User findById(String id);
    User save(User data);
    User update(String id, User data);
    void delete(String id);
}
