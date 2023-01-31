package com.example.demo.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@NoArgsConstructor
public class MemberDto {

    private String username;
    private int age;
    
    @QueryProjection
    @Builder
    public MemberDto(String username, int age) {
        this.username = username;
        this.age = age;
    }
    
    
}
