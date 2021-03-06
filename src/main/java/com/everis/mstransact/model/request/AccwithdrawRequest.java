package com.everis.mstransact.model.request;
 
 

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class AccwithdrawRequest { 
	private String id;
	private String prodtype; 
	private String titular;
	private Double amount;
	private Double commission; 
}
