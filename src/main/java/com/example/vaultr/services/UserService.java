package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.UserResponseDTO;
import com.example.vaultr.entities.User;
import com.example.vaultr.repositories.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

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

    @Override
    public UserResponseDTO getUserById(Long id) throws Exception {
        User user =  userRepository.findById(id).orElseThrow(()-> new Exception ("User Not Found"));
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }

    @Override
    public UserResponseDTO getUserByName(String name) throws Exception {
        User user =  userRepository.findByName(name);
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
