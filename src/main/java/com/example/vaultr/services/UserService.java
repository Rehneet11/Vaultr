package com.example.vaultr.services;

import com.example.vaultr.dto.CreateUserRequestDTO;
import com.example.vaultr.dto.CreateUserResponseDTO;
import com.example.vaultr.dto.CreateWalletRequestDTO;
import com.example.vaultr.dto.UserResponseDTO;
import com.example.vaultr.entities.User;
import com.example.vaultr.entities.Wallet;
import com.example.vaultr.repositories.UserRepository;
import com.example.vaultr.repositories.WalletRepository;
import com.example.vaultr.utils.IdGenerator;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService implements IUserService{
    private final UserRepository userRepository;
    private final IWalletService walletService;

    public UserService(UserRepository userRepository, IWalletService walletService) {
        this.userRepository = userRepository;
        this.walletService = walletService;
    }

    @Override
    public CreateUserResponseDTO createUser(CreateUserRequestDTO userRequestDTO) throws Exception {
        User user0 = userRepository.findByEmail(userRequestDTO.getEmail());
        if( user0 != null){
            throw new Exception("User Already Exists");
        }
        User user = User.builder()
                .id(IdGenerator.generateId())
                .name(userRequestDTO.getName())
                .email(userRequestDTO.getEmail())
                .build();
        User savedUser = userRepository.save(user);
        CreateWalletRequestDTO walletRequestDTO = CreateWalletRequestDTO.builder().userId(savedUser.getId()).build();
        Wallet wallet = walletService.createWallet(savedUser);
        return CreateUserResponseDTO.builder()
                .userId(savedUser.getId())
                .walletId(wallet.getId())
                .build();
    }

    @Override
    public UserResponseDTO getUserById(String id) throws Exception {
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

    @Override
    public UserResponseDTO getUserByEmail(String email) throws Exception {
        User user = userRepository.findByEmail(email);
        return UserResponseDTO.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .build();
    }
}
