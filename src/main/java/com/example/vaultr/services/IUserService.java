package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;

public interface IUserService {
    CreateUserResponseDTO createUser(CreateUserRequestDTO userRequestDTO);
}
