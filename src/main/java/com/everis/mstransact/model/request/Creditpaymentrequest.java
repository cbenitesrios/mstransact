package com.everis.mstransact.model.request;
 

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@Setter
@ToString
public class Creditpaymentrequest {
	private String id;
	private String prodtype; 
	private String titular;
	private Double amount;
	
}
