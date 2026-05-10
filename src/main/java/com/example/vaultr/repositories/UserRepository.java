package com.example.vaultr.repositories;

import com.example.vaultr.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository <User,String> {
    User findByName(String name);
    User findByEmail(String email);
}
