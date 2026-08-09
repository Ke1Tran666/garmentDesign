package com.garmentDesign.service;

import com.garmentDesign.entity.User;

public interface UserStatusService {

	User refreshStatus(User user);

	User refreshStatus(String idUser);
}