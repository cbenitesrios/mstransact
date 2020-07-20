package com.everis.mstransact.service.impl;
 
import java.time.LocalDate;   
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient; 
import com.everis.mstransact.model.Transaction;
import com.everis.mstransact.model.dto.AccountDto;
import com.everis.mstransact.model.dto.CreditDto;
import com.everis.mstransact.model.request.AccdepositRequest;
import com.everis.mstransact.model.request.AccwithdrawRequest;
import com.everis.mstransact.model.request.Creditconsumerequest;
import com.everis.mstransact.model.request.Creditpaymentrequest;
import com.everis.mstransact.model.request.Updatetransactionreq;
import com.everis.mstransact.repository.ITransactionrepo;
import com.everis.mstransact.service.IMstransacservice; 
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
 
@Service
public class MstransacserviceImpl implements IMstransacservice{
	
	@Autowired
	private ITransactionrepo transacrepo;
	
	private static final Double COMMISSION_WITHDRAW_VALUE= 10d;
	private static final Double COMMISSION_DEPOSIT_VALUE= 10d;
	private static final Long COMMISSION_FREE_TIMES= 5l;

	@Override
	public Mono<Transaction> moneywithdraw(AccwithdrawRequest mwithdrawrequest, Mono<AccountDto> account, WebClient webclient) { 
		return account.filter(acc-> acc.getTitular().contains(mwithdrawrequest.getTitular()))
				      .switchIfEmpty(Mono.error(new Exception("Titular not found")))
				      .flatMap(acc-> 
				    	  transacrepo.countByTitular(mwithdrawrequest.getTitular()).switchIfEmpty(Mono.error(new Exception("problema"))).log().map(count ->
				    	  {   mwithdrawrequest.setCommission(count>=COMMISSION_FREE_TIMES?COMMISSION_WITHDRAW_VALUE:0);  
				    	      return acc;
				    	  }) 
				      )
				      .filter(acc-> acc.getSaldo()-mwithdrawrequest.getAmount()-mwithdrawrequest.getCommission()>=0)
				      .switchIfEmpty(Mono.error(new Exception("Dont have enought money")))
				      .flatMap(refresh-> {
				    	  refresh.setSaldo(refresh.getSaldo()- mwithdrawrequest.getAmount()-mwithdrawrequest.getCommission());
				    	  return webclient.put().body(BodyInserters.fromValue(refresh)).retrieve().bodyToMono(AccountDto.class);
				      })
				      .switchIfEmpty(Mono.error(new Exception("Error refresh account")))
				      .flatMap(then->            transacrepo.save(Transaction.builder()
							                    .prodid(mwithdrawrequest.getId())
							                    .prodtype(mwithdrawrequest.getProdtype())
							                    .transtype("WITHDRAW")
							                    .titular(mwithdrawrequest.getTitular())
							                    .amount(mwithdrawrequest.getAmount())
							                    .commission(mwithdrawrequest.getCommission())
							                    .postamount(then.getSaldo())
							                    .build())); 
	}
	
	@Override
	public Mono<Transaction> moneydeposit(AccdepositRequest mdepositrequest, Mono<AccountDto> account, WebClient webclient) {
		 
		return account.filter(acc-> acc.getTitular().contains(mdepositrequest.getTitular()))
		              .switchIfEmpty(Mono.error(new Exception("Titular not found")))
				      .flatMap(acc-> transacrepo.countByTitular(mdepositrequest.getTitular())
				    		  .flatMap(count ->
				              {      mdepositrequest.setCommission(count>=COMMISSION_FREE_TIMES?COMMISSION_DEPOSIT_VALUE:0); 
				    		         acc.setSaldo(acc.getSaldo() + mdepositrequest.getAmount()-mdepositrequest.getCommission());
					    	         return webclient.put().body(BodyInserters.fromValue(acc)).retrieve().bodyToMono(AccountDto.class);
			                  })
				      ) 
				      .switchIfEmpty(Mono.error(new Exception("Error refresh account")))
				      .flatMap(then->               transacrepo.save(Transaction.builder()
								                    .prodid(then.getId())
								                    .prodtype(then.getAcctype())
								                    .transtype("DEPOSIT")
								                    .titular(mdepositrequest.getTitular())
								                    .amount(mdepositrequest.getAmount())
								                    .commission(mdepositrequest.getCommission())
								                    .postamount(then.getSaldo())
								                    .build()));
	}

	@Override
	public Mono<Transaction> creditpayment(Creditpaymentrequest cpaymentrequest, Mono<CreditDto> credit, WebClient credwebclient) { 
		return  credit.filter(cred-> cred.getTitular().contains(cpaymentrequest.getTitular()))
				.switchIfEmpty(Mono.error(new Exception("Not credit found - cpayment"))) 
				.filter(cred -> cred.getConsume()-cpaymentrequest.getAmount()>=0)
				.switchIfEmpty(Mono.error(new Exception("Cant process the transaction")))
				.flatMap(refresh -> {
					refresh.setConsume(refresh.getConsume()-cpaymentrequest.getAmount());
					return credwebclient.put().body(BodyInserters.fromValue(refresh)).retrieve().bodyToMono(CreditDto.class) ;
				})
				 .switchIfEmpty(Mono.error(new Exception("Error refresh credit")))
				 .flatMap(then -> transacrepo.save(Transaction.builder()
								                    .prodid(then.getId())
								                    .prodtype(then.getCredittype())
								                    .transtype("PAYMENT")
								                    .titular(cpaymentrequest.getTitular())
								                    .amount(cpaymentrequest.getAmount())
								                    .postamount(then.getBaseline()-then.getConsume())
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
				     .flatMap(then-> transacrepo.save(Transaction.builder()
								                    .prodid(then.getId())
								                    .prodtype(then.getCredittype())
								                    .transtype("CONSUME")
								                    .titular(cconsumerequest.getTitular())
								                    .amount(cconsumerequest.getAmount())
								                    .postamount(then.getBaseline()-then.getConsume())
								                    .build()));
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
	                   	   .amount(updatetransacreq.getAmount())
	                   	   .commission(updatetransacreq.getCommision())
	                   	   .postamount(updatetransacreq.getPostamount()) 
	                       .build()));
	}
	 

}
