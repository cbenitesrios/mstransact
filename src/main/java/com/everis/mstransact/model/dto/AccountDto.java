package com.everis.mstransact.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;  
 

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {

	private String id;  
	private String acctype;
	private List<String> titular;
	private List<String> firmantecode;
	private Double saldo;
	private String acctypedesc; 
	
}
