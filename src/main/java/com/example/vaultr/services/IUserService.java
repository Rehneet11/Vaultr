package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.UserResponseDTO;

public interface IUserService {
    CreateUserResponseDTO createUser(CreateUserRequestDTO userRequestDTO) throws Exception;
    UserResponseDTO getUserById(String id) throws Exception;
    UserResponseDTO getUserByName(String name) throws Exception;
    UserResponseDTO getUserByEmail(String email) throws Exception;
}
