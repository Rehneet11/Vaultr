package com.example.vaultr.controllers;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.UserResponseDTO;
import com.example.vaultr.services.IUserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/user")
public class UserController {
    private final IUserService userService;

    public UserController(IUserService userService) {
        this.userService = userService;
    }

    @PostMapping("/create-user")
    public CreateUserResponseDTO createUser(@Valid @RequestBody CreateUserRequestDTO userRequestDTO) throws Exception {
        return userService.createUser(userRequestDTO);
    }

    @GetMapping("/{id}")
    public UserResponseDTO getUserById(@PathVariable String id) throws Exception{
        return userService.getUserById(id);
    }

    @GetMapping("/name/{name}")
    public UserResponseDTO getUserByName(@PathVariable String name) throws Exception {
        return userService.getUserByName(name);
    }

    @GetMapping("/email/{email}")
    public UserResponseDTO getUserByEmail(@PathVariable String email) throws Exception {
        return userService.getUserByEmail(email);
    }
}
