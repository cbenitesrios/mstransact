package com.everis.mstransact.model.dto;

import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorDto extends Exception  {
    private String code;
    private String message; 
    
}