package com.everis.mstransact.repository;
 

import java.time.LocalDate;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.everis.mstransact.model.Transaction;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ITransactionrepo extends ReactiveMongoRepository<Transaction, String>{

	Flux<Transaction> findByTitularAndTransactdateBetween(String titular, LocalDate date1, LocalDate date2);
	Mono<Long> countByTitular(String titular);
	Mono<Long> countByTitularAndProdid(String titular, String productid);
	
}
