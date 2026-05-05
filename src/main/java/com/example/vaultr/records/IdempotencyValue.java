package com.example.vaultr.records;

public record IdempotencyValue(int status, String responseBody, boolean isDone) {
    public static IdempotencyValue inProgress(){
        return new IdempotencyValue(0,null,false);
    }
    public static IdempotencyValue completed(int status, String responseBody){
        return new IdempotencyValue(status,responseBody,true);
    }
}
