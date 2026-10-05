package com.yourteam.lostfound.service.impl;

import com.yourteam.lostfound.dto.UserLoginDTO;
import com.yourteam.lostfound.dto.UserRegisterDTO;
import com.yourteam.lostfound.exception.BadRequestException;
import com.yourteam.lostfound.exception.ResourceNotFoundException;
import com.yourteam.lostfound.model.User;
import com.yourteam.lostfound.repository.UserRepository;
import com.yourteam.lostfound.service.UserService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class UserServiceImpl implements UserService, UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User registerUser(UserRegisterDTO dto) {
        if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
            throw new BadRequestException("Email cannot be empty");
        }
        if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
            throw new BadRequestException("Password cannot be empty");
        }

        String normalizedEmail = dto.getEmail().trim().toLowerCase();

        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new BadRequestException("Email is already registered: " + normalizedEmail);
        }

        // Strict security: Public self-registration ALWAYS receives ROLE_USER
        String assignedRole = "ROLE_USER";

        User user = new User(
                dto.getName() != null ? dto.getName().trim() : "",
                normalizedEmail,
                passwordEncoder.encode(dto.getPassword()),
                assignedRole
        );

        return userRepository.save(user);
    }

    @Override
    public User authenticateUser(UserLoginDTO loginDTO) {
        String normalizedEmail = loginDTO.getEmail() != null ? loginDTO.getEmail().trim().toLowerCase() : "";
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadRequestException("Invalid email or password"));

        // Supports both BCrypt encoded passwords and legacy plaintext matches
        boolean passwordMatches = passwordEncoder.matches(loginDTO.getPassword(), user.getPassword())
                || user.getPassword().equals(loginDTO.getPassword());

        if (!passwordMatches) {
            throw new BadRequestException("Invalid email or password");
        }

        return user;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        String role = user.getRole();
        if (role == null || role.isBlank()) role = "ROLE_USER";
        if (!role.startsWith("ROLE_")) role = "ROLE_" + role;

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(role))
        );
    }

    @Override
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    }

    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Override
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Override
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        userRepository.delete(user);
    }

    @Override
    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new com.yourteam.lostfound.exception.UnauthorizedException("No authenticated user found in security context");
        }

        String email = authentication.getName();
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword()) && !user.getPassword().equals(oldPassword)) {
            throw new BadRequestException("Current password does not match.");
        }
        if (newPassword == null || newPassword.trim().length() < 6) {
            throw new BadRequestException("New password must be at least 6 characters long.");
        }
        user.setPassword(passwordEncoder.encode(newPassword.trim()));
        userRepository.save(user);
    }

    @Override
    public User updateProfile(Long userId, String name, String phoneNumber) {
        User user = getUserById(userId);
        if (name != null && !name.trim().isEmpty()) {
            user.setName(name.trim());
        }
        if (phoneNumber != null) {
            user.setPhoneNumber(phoneNumber.trim());
        }
        return userRepository.save(user);
    }
}