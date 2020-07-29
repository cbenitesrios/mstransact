package com.everis.mstransact.repository;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.everis.mstransact.model.Consume;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface IConsumeRepo extends ReactiveMongoRepository<Consume, String>{
	
	Flux<Consume> findByProductidAndPayedOrderByMonthAsc(String productid, Boolean payed);

}
