package com.everis.mstransact.model.dto;
  
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor  
public class CreditDto {
 
	private String id;
	private String bank;
	private String titular;
	private String credittype;
	private String credittypedesc; 
	private Double baseline;
	private Double consume; 
}
