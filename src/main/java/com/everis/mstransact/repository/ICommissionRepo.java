package com.everis.mstransact.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import com.everis.mstransact.model.Commission;

import reactor.core.publisher.Mono;

public interface ICommissionRepo  extends ReactiveMongoRepository<Commission, String>{
	Mono<Commission> findByBankAndProduct(String bank, String product);
}
