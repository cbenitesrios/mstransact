package com.everis.mstransact.service; 
import org.springframework.web.reactive.function.client.WebClient;

import com.everis.mstransact.model.Transaction;
import com.everis.mstransact.model.dto.AccountDto;
import com.everis.mstransact.model.dto.CreditDto;
import com.everis.mstransact.model.request.AccdepositRequest;
import com.everis.mstransact.model.request.AccwithdrawRequest;
import com.everis.mstransact.model.request.Creditconsumerequest;
import com.everis.mstransact.model.request.Creditpaymentrequest; 

import reactor.core.publisher.Mono;

public interface IMstransacservice {
  Mono<Transaction> moneywithdraw(AccwithdrawRequest mwithdrawrequest, Mono<AccountDto> account,WebClient accwebclient);
  Mono<Transaction> moneydeposit(AccdepositRequest mdepositrequest, Mono<AccountDto> account, WebClient accwebclient);
  Mono<Transaction> creditpayment(Creditpaymentrequest cpaymentrequest, Mono<CreditDto> account, WebClient credwebclient);
  Mono<Transaction> creditconsume(Creditconsumerequest cpaymentrequest, Mono<CreditDto> account, WebClient credwebclient);
  
  /*Mono<Void> deletetransaction(String id); 
  Flux<Transaction> findtransaction();
  Flux<Transaction> findclienttransaction(String titular);
  Mono<Transaction> findtransactionbyid(String id);
  Mono<Transaction> updatetransaction(Updatetransactionreq updatetransactionreq);*/
 
}
