package com.example.vaultr.saga;

import com.example.vaultr.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Builder
@NoArgsConstructor
@Data
public class SAGAContext {
    private Map<String, Object> context;
    public SAGAContext(Map<String,Object> context){
        if(context!=null){
            this.context=new HashMap<>(context);
        }
        else{
            this.context=new HashMap<>();
        }
    }
    public void addContext(String key, Object value){
        context.put(key,value);
    }
    public Object getContext(String key){
        return context.get(key);
    }


    public BigDecimal getBigDecimal(String key){
        Object val = getContext(key);
        if(val instanceof Number){
            return BigDecimal.valueOf(((Number) val).doubleValue());
        }
        return null;
    }
    public String getString(String key){
        Object val =getContext(key);
        if(val instanceof String){
            return String.valueOf(val);
        }
        return null;
    }
    public TransactionStatus getTransactionStatusEnum(String key) {
        Object val = getContext(key);
        if (val instanceof TransactionStatus status) {
            return status;
        }
        if (val instanceof String str) {
            return TransactionStatus.valueOf(str);
        }
        return null;
    }
}
