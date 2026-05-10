package com.example.vaultr.utils;

import com.github.f4b6a3.ulid.UlidCreator;

public class IdGenerator {

    public static String generateId() {
        return UlidCreator.getUlid().toString();
    }
}
