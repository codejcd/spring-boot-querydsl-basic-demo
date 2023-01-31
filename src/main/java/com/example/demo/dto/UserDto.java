package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@ToString
public class UserDto {
    
    private String name;
    private int age;
    
    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
                                      
    }
}
