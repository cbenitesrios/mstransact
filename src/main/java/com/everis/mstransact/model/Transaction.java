package com.everis.mstransact.model;
 

import java.sql.Date;

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
	private String prodtype; 
	private String transtype;
	private String titular;
	private Date transactdate;
	private Double amount; 
	private Double postamount; 
}
