package com.everis.mstransact.service.impl;
 
import java.time.LocalDate;
import java.util.Iterator;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import com.everis.mstransact.config.Configtransaction;
import com.everis.mstransact.model.Consume;
import com.everis.mstransact.model.Transaction;
import com.everis.mstransact.model.dto.AccountDto;
import com.everis.mstransact.model.dto.CreditDto;
import com.everis.mstransact.model.request.AccdepositRequest;
import com.everis.mstransact.model.request.AccwithdrawRequest;
import com.everis.mstransact.model.request.Creditconsumerequest;
import com.everis.mstransact.model.request.Creditpaymentrequest;
import com.everis.mstransact.model.request.Transferpaymentrequest;
import com.everis.mstransact.model.request.Updatetransactionreq;
import com.everis.mstransact.repository.IConsumeRepo;
import com.everis.mstransact.repository.ITransactionrepo;
import com.everis.mstransact.service.IMstransacservice;
import com.google.common.util.concurrent.AtomicDouble;
 
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
  
@Service
public class MstransacserviceImpl implements IMstransacservice{
	
	@Autowired
	private ITransactionrepo transacrepo;
	
	@Autowired
	private IConsumeRepo consumerepo;

	@Override
	public Mono<Transaction> moneywithdraw(AccwithdrawRequest mwithdrawrequest, Mono<AccountDto> account, WebClient webclient) { 
		return account.filter(acc-> acc.getTitular().contains(mwithdrawrequest.getTitular()))
				      .switchIfEmpty(Mono.error(new Exception("Titular not found")))
				      .flatMap(acc-> 
				    	  transacrepo.countByTitular(mwithdrawrequest.getTitular()).switchIfEmpty(Mono.error(new Exception("problema"))).log().map(count ->
				    	  {   mwithdrawrequest.setCommission(count>=Configtransaction.COMMISSION_FREE_TIMES?Configtransaction.COMMISSION_WITHDRAW_VALUE:0);  
				    	      return acc;
				    	  })) 
				      .filter(acc-> acc.getBalance()-mwithdrawrequest.getAmount()-mwithdrawrequest.getCommission()>=0)
				      .switchIfEmpty(Mono.error(new Exception("Dont have enought money")))
				      .flatMap(refresh-> {
				    	  refresh.setBalance(refresh.getBalance()- mwithdrawrequest.getAmount()-mwithdrawrequest.getCommission());
				    	  return webclient.put().body(BodyInserters.fromValue(refresh)).retrieve().bodyToMono(AccountDto.class);
				      })
				      .switchIfEmpty(Mono.error(new Exception("Error refresh account")))
				      .flatMap(then->            transacrepo.save(Transaction.builder()
							                    .prodid(then.getId())
							                    .prodtype(then.getAcctype())
							                    .transtype("WITHDRAW")
							                    .titular(mwithdrawrequest.getTitular())
							                    .amount(mwithdrawrequest.getAmount())
							                    .commission(mwithdrawrequest.getCommission())
							                    .postamount(then.getBalance()) 
							                    .build())); 
	}
	
	@Override
	public Mono<Transaction> moneydeposit(AccdepositRequest mdepositrequest, Mono<AccountDto> account, WebClient webclient) {
		return account.filter(acc-> acc.getTitular().contains(mdepositrequest.getTitular()))
		              .switchIfEmpty(Mono.error(new Exception("Titular not found")))
				      .flatMap(acc-> transacrepo.countByTitular(mdepositrequest.getTitular())
				    		  .flatMap(count ->
				              {      mdepositrequest.setCommission(count>=Configtransaction.COMMISSION_FREE_TIMES?Configtransaction.COMMISSION_DEPOSIT_VALUE:0); 
				    		         acc.setBalance(acc.getBalance() + mdepositrequest.getAmount()-mdepositrequest.getCommission());
					    	         return webclient.put().body(BodyInserters.fromValue(acc)).retrieve().bodyToMono(AccountDto.class);
			                  })
				      ) 
				      .doOnNext(System.out::println)
				      .switchIfEmpty(Mono.error(new Exception("Error refresh account")))
				      .flatMap(then->               transacrepo.save(Transaction.builder()
								                    .prodid(then.getId())
								                    .prodtype(then.getAcctype())
								                    .transtype("DEPOSIT")
								                    .titular(mdepositrequest.getTitular())
								                    .amount(mdepositrequest.getAmount())
								                    .commission(mdepositrequest.getCommission())
								                    .postamount(then.getBalance())
								                    .build()));
	}
	
	@Override
	public Mono<Transaction> creditconsume(Creditconsumerequest cconsumerequest,  Mono<CreditDto> credit, WebClient credwebclient) { 
		return credit.filter(cred-> cred.getTitular().contains(cconsumerequest.getTitular()))
		             .switchIfEmpty(Mono.error(new Exception("Not credit found  - cconsume"))) 
				     .filter(cred -> (cred.getBaseline()-cred.getConsume()-cconsumerequest.getAmount())>=0)
				     .switchIfEmpty(Mono.error(new Exception("Cant process the transaction")))
				     .flatMap(refresh -> {
					  refresh.setConsume(refresh.getConsume()+cconsumerequest.getAmount());
					  return credwebclient.put().body(BodyInserters.fromValue(refresh)).retrieve().bodyToMono(CreditDto.class) ;
					 }) 
				     .flatMap(then->  consumerepo.save(Consume.builder()
				    		   .amount(cconsumerequest.getAmount())
				    		   .notpayedamount(cconsumerequest.getAmount())
	                           .productid(then.getId())
	                           .titular(then.getTitular())
	                           .month(LocalDate.now())
	                           .maxmonth(LocalDate.of(LocalDate.now().getYear(),LocalDate.now().plusMonths(1L).getMonth(),LocalDate.now().plusMonths(1L).lengthOfMonth()))
	                           .payed(false)
	                           .build()).thenReturn(then)
	                    )
				     .flatMap(then->  transacrepo.save(Transaction.builder()
								                    .prodid(then.getId())
								                    .prodtype(then.getCredittype())
								                    .transtype("CONSUME")
								                    .titular(cconsumerequest.getTitular())
								                    .amount(cconsumerequest.getAmount())
								                    .postamount(then.getBaseline()-then.getConsume())
								                    .build())
				    	);
	}

	@Override
	public Mono<Transaction> creditpayment(Creditpaymentrequest cpaymentrequest, Mono<CreditDto> credit, WebClient credwebclient) { 
		return  credit.filter(cred-> cred.getTitular().contains(cpaymentrequest.getTitular())) 
				.switchIfEmpty(Mono.error(new Exception("Not credit found - cpayment"))) 
				.filter(cred -> cred.getConsume()-cpaymentrequest.getAmount()>=0)
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction")))
				.flatMap(refresh -> {
					refresh.setConsume(refresh.getConsume()-cpaymentrequest.getAmount());
					return credwebclient.put().body(BodyInserters.fromValue(refresh)).retrieve().bodyToMono(CreditDto.class);
				})
				 .switchIfEmpty(Mono.error(new Exception("Error refresh credit")))
				 .flatMap(then -> transacrepo.save(Transaction.builder()
		                    .prodid(then.getId())
		                    .prodtype(then.getCredittype())
		                    .transtype("PAYMENT")
		                    .titular(cpaymentrequest.getTitular())
		                    .amount(cpaymentrequest.getAmount())
		                    .postamount(then.getBaseline()-then.getConsume())
		                    .build())) 
				 .flatMap(transaction-> {
					AtomicDouble amountss=new AtomicDouble();
					amountss.set(transaction.getAmount());
					return consumerepo.findByProductidAndPayedOrderByMonthAsc(transaction.getProdid(), false)  
					                  .map(consum ->{ 
					                	  if(amountss.doubleValue()>=consum.getNotpayedamount()){ 
					                		  amountss.set(amountss.doubleValue()-consum.getNotpayedamount());
					                		  consum.setNotpayedamount(0d);
					                		  consum.setPayed(true);
					                 	  }else{ 
					                	      consum.setNotpayedamount(consum.getNotpayedamount()-amountss.get());
					                		  amountss.set(0d); 
					                	  } 
					                	  return consum; 
					                   }).flatMap(consumerepo::save).then()
					                  .thenReturn(transaction); 
				  });		
	}

	@Override
	public Mono<Void> deletetransaction(String id) { 
		return transacrepo.findById(id)
				.switchIfEmpty(Mono.error(new Exception("No encontrado")))
				.flatMap(transacrepo::delete);
	}

	@Override
	public Flux<Transaction> findclienttransaction(String titular, LocalDate date1, LocalDate date2) { 
		return transacrepo.findByTitularAndTransactdateBetween(titular,date1,date2)
				          .switchIfEmpty(Mono.error(new Exception("Not found transaction")));
	}
	
	@Override
	public Flux<Transaction> findtransaction() { 
		return transacrepo.findAll();
	}
	
	@Override
	public Mono<Transaction> findtransactionbyid(String id) { 
		return transacrepo.findById(id)
				          .switchIfEmpty(Mono.error(new Exception("Not found transaction")));
	} 
	
	@Override
	public Mono<Transaction> updatetransaction(Updatetransactionreq updatetransacreq) { 
		return transacrepo.findById(updatetransacreq.getId())
				.switchIfEmpty(Mono.error(new Exception("not found")))
				.flatMap(a-> transacrepo.save(Transaction.builder()
	                       .id(a.getId())
	                       .prodid(updatetransacreq.getId())
	                   	   .prodtype(updatetransacreq.getProdtype()) 
	                   	   .transtype(updatetransacreq.getTranstype())
	                   	   .titular(updatetransacreq.getTitular())
	                   	   .transactdate(updatetransacreq.getTransactdate())
	                   	   .amount(updatetransacreq.getAmount())
	                   	   .commission(updatetransacreq.getCommission())
	                   	   .postamount(updatetransacreq.getPostamount()) 
	                       .build()));
	}

	@Override
	public Mono<Transaction> transferpayment(Transferpaymentrequest tpaymentrequest, Mono<AccountDto> account,
			Mono<CreditDto> credit, WebClient accwebclient, WebClient credwebclient) { 
		return  account.filter(acc->acc.getTitular().contains(tpaymentrequest.getAccounttitular()))
				.switchIfEmpty(Mono.error(new Exception("Not same account holder - transferpayment")))
				.flatMap(acc-> 
		    	  transacrepo.countByTitular(tpaymentrequest.getAccounttitular()).switchIfEmpty(Mono.error(new Exception("problema"))).map(count ->
		    	  {   tpaymentrequest.setCommission(count>=Configtransaction.COMMISSION_FREE_TIMES?Configtransaction.COMMISSION_WITHDRAW_VALUE:0);  
		    	      return acc;
		    	  }) 
		         )
				.filter(acc-> acc.getBalance()-tpaymentrequest.getAmount()-tpaymentrequest.getCommission()>=0)
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction - low account balance")))
				.then(credit)
				.filter(cred-> cred.getTitular().contains(tpaymentrequest.getCredittitular()))
				.switchIfEmpty(Mono.error(new Exception("Not same credit holder - transferpayment")))
				.filter(cred -> (cred.getConsume()-tpaymentrequest.getAmount()-tpaymentrequest.getCommission())>=0)
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction - amount")))
				.flatMap(cre->{
					 cre.setConsume(cre.getConsume()-tpaymentrequest.getAmount()-tpaymentrequest.getCommission());
					 return credwebclient.put().body(BodyInserters.fromValue(cre)).retrieve().bodyToMono(CreditDto.class) ;
				})
				.switchIfEmpty(Mono.error(new Exception("Cant process the transcation - credit")))
				.flatMap(cre->transacrepo.save(Transaction.builder()
	                         .prodid(cre.getId())
	                   	     .prodtype(tpaymentrequest.getProdtype()) 
	                   	     .transtype("TRANSPAYMENT")
	                   	     .titular(tpaymentrequest.getCredittitular())
	                   	     .amount(tpaymentrequest.getAmount())
	                   	     .commission(tpaymentrequest.getCommission())
	                   	     .postamount(cre.getConsume()-tpaymentrequest.getAmount()-tpaymentrequest.getCommission()) 
	                         .build()))
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction - creditransaction")))
				.then(account)
				.flatMap(acc-> {
			    	  acc.setBalance(acc.getBalance()- tpaymentrequest.getAmount()-tpaymentrequest.getCommission());
			    	  return accwebclient.put().body(BodyInserters.fromValue(acc)).retrieve().bodyToMono(AccountDto.class);
			      })
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction - account")))
				.flatMap(then->            transacrepo.save(Transaction.builder()
	                    .prodid(then.getId())
	                    .prodtype(then.getAcctype())
	                    .transtype("TRANSWITHDRAW")
	                    .titular(tpaymentrequest.getAccounttitular())
	                    .amount(tpaymentrequest.getAmount())
	                    .commission(tpaymentrequest.getCommission())
	                    .postamount(then.getBalance())
	                    .build())); 		 
				 
			 
	}
	
	@Override
	public Mono<Boolean> checkforexpiredcredit(String titular){
		return consumerepo.findByTitularAndPayed(titular, false) 
				          .collectList()
				          .map(cons -> cons.stream().filter(a-> a.getMaxmonth().isBefore(LocalDate.now())).count()>0);
				        		  
	}
	 

}
