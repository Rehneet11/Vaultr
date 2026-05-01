package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.UserResponseDTO;

public interface IUserService {
    CreateUserResponseDTO createUser(CreateUserRequestDTO userRequestDTO);
    UserResponseDTO getUserById(Long id) throws Exception;
    UserResponseDTO getUserByName(String name) throws Exception;
}
