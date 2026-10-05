package com.yourteam.lostfound.service;

import com.yourteam.lostfound.dto.UserLoginDTO;
import com.yourteam.lostfound.dto.UserRegisterDTO;
import com.yourteam.lostfound.model.User;

import java.util.List;

public interface UserService {

    User registerUser(UserRegisterDTO registerDTO);

    User authenticateUser(UserLoginDTO loginDTO);

    User getUserById(Long id);

    User getUserByEmail(String email);

    List<User> getAllUsers();

    void deleteUser(Long id);

    User getCurrentAuthenticatedUser();

    void changePassword(Long userId, String oldPassword, String newPassword);

    User updateProfile(Long userId, String name, String phoneNumber);
}