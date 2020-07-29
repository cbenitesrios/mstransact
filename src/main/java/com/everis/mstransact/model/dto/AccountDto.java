package com.everis.mstransact.model.dto;

import java.util.List; 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;  
 

@Builder 
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountDto {
  
	private String id;      
	private List<String> titular;
	private List<String> firmantecode; 
	private String bank;   
	private String acctype;
	private String accdescription; 
	private Double balance;  
}
