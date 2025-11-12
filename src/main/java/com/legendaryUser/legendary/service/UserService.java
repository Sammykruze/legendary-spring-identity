package com.legendaryUser.legendary.service;


import com.legendaryUser.legendary.dto.UserProfileDto;
import com.legendaryUser.legendary.exception.ResourceNotFoundException;
import com.legendaryUser.legendary.model.User;
import com.legendaryUser.legendary.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    public UserProfileDto getUserProfileById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return convertToDto(user);
    }

    public List<UserProfileDto> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    // Simple helper method to convert a User entity to a UserProfileDto
    private UserProfileDto convertToDto(User user) {
        return new UserProfileDto(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.getCreatedAt()
        );
    }
}

