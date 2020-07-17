package com.everis.mstransact.model.request.product;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class UpdateAccountRequest{ 
	private String id;   
	private String acctype; 
	private List<String> titular;
	private List<String> firmante;
	private Double saldo;
}	