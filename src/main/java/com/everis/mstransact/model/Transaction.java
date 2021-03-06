package com.everis.mstransact.model;
import java.time.LocalDateTime;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document; 

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString; 
 
@Data 
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document
@ToString 
public class Transaction{ 
	
	@Id
	private String id;
	private String prodid;
	@NotNull(message = "no puede ser nulo")
	private String prodtype; 
	private String transtype; 	
	private String titular;
	private String bank;
	@Builder.Default
	private LocalDateTime transactdate= LocalDateTime.now();
	private Double amount; 
	private Double commission;
	private Double postamount; 
}
