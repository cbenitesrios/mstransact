package com.everis.mstransact.model.dto;

import java.util.List;
 
 
import lombok.Data; 
import lombok.NoArgsConstructor;
 

@Data
@NoArgsConstructor
public class AccountDto {

	private String id;  
	private String acctype;
	private List<String> titular;
	private List<String> firmantecode;
	private Double saldo;



}
