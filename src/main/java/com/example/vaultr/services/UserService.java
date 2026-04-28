package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.entities.User;
import com.example.vaultr.repositories.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService implements IUserService{
    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public CreateUserResponseDTO createUser(CreateUserRequestDTO userRequestDTO) {
        User user = User.builder()
                .name(userRequestDTO.getName())
                .email(userRequestDTO.getEmail())
                .build();
        User savedUser = userRepository.save(user);
        return CreateUserResponseDTO.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .build();
    }
}
