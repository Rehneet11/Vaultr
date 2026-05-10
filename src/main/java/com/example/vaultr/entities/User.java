package com.example.vaultr.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "user")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
public class User extends BaseEntity {
    @Id
    @Column(length = 26, nullable = false, updatable = false)
    public String id;

    private String name;

    private String email;
}
