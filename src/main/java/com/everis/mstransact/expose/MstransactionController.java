package com.everis.mstransact.expose;
 
import java.time.LocalDate; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus; 
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient; 
import com.everis.mstransact.model.Transaction;
import com.everis.mstransact.model.dto.AccountDto;
import com.everis.mstransact.model.dto.AtmtransactDto;
import com.everis.mstransact.model.dto.CreditDto;
import com.everis.mstransact.model.request.AccdepositRequest;
import com.everis.mstransact.model.request.AccwithdrawRequest;
import com.everis.mstransact.model.request.Creditconsumerequest;
import com.everis.mstransact.model.request.Creditpaymentrequest;
import com.everis.mstransact.model.request.Transferpaymentrequest;
import com.everis.mstransact.model.request.Updatetransactionreq;
import com.everis.mstransact.model.response.TransactionResponse;
import com.everis.mstransact.service.IMstransacservice;  
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/apitransaction")
public class MstransactionController {
	
	@Autowired
	private IMstransacservice transacservice;
	private static final String URL_ACCOUNT= "http://localhost:8030/apiaccount";
	private static final String URL_CREDIT= "http://localhost:8030/apicredit";
	
	
	/*Realizar un retiro de dinero de una cuenta*/
	@PostMapping("/withdraw")
	public  Mono<Transaction> moneywithdraw(@RequestBody AccwithdrawRequest mwithdrawrequest){ 
		Mono<AccountDto> accountReq = WebClient.create(URL_ACCOUNT + "/findacc/"+mwithdrawrequest.getId())
				                            .get().retrieve().bodyToMono(AccountDto.class);  
		return transacservice.moneywithdraw(mwithdrawrequest,accountReq, WebClient.create(URL_ACCOUNT+ "/updateaccount"));
	}
	/*Depositar a una cuenta*/
	@PostMapping("/deposit")
	public Mono<Transaction> moneydeposit(@RequestBody AccdepositRequest mdepositrequest){
		Mono<AccountDto> accountReq = WebClient.create( URL_ACCOUNT + "/findacc/"+mdepositrequest.getId())
                .get().retrieve().bodyToMono(AccountDto.class);
		return transacservice.moneydeposit(mdepositrequest, accountReq, WebClient.create(URL_ACCOUNT + "/updateaccount"));
	}
	
	/*Depositar a una cuenta*/
	@PostMapping("/payment")
	public Mono<Transaction> creditpayment(@RequestBody Creditpaymentrequest cpaymentrequest){
		Mono<CreditDto> credit = WebClient.create( URL_CREDIT + "/findcred/"+cpaymentrequest.getId())
                .get().retrieve().bodyToMono(CreditDto.class);
		return transacservice.creditpayment(cpaymentrequest, credit, WebClient.create(URL_CREDIT + "/updatecredit"));
	}
	
	/*Pago de un credito mediante transferencia de una cuenta*/
	@PostMapping("/transferpayment")
	public Mono<Transaction> transferpayment(@RequestBody Transferpaymentrequest tpaymentrequest){
		Mono<AccountDto> account = WebClient.create( URL_ACCOUNT + "/findacc/"+tpaymentrequest.getAccountid())
                .get().retrieve().bodyToMono(AccountDto.class); 
		Mono<CreditDto> credit = WebClient.create( URL_CREDIT + "/findcred/"+tpaymentrequest.getCreditid())
                .get().retrieve().bodyToMono(CreditDto.class);
		return transacservice.transferpayment(tpaymentrequest, account, credit, WebClient.create(URL_ACCOUNT + "/updateaccount"), WebClient.create(URL_CREDIT + "/updatecredit"));
	}
	 
	/*Realizar un consumo a un producto de credito*/
	@PostMapping("/consume")
	public Mono<Transaction> creditconsume(@RequestBody Creditconsumerequest cconsumerequest){
		Mono<CreditDto> credit = WebClient.create( URL_CREDIT + "/findcred/"+cconsumerequest.getId())
                .get().retrieve().bodyToMono(CreditDto.class);
		return transacservice.creditconsume(cconsumerequest, credit, WebClient.create(URL_CREDIT + "/updatecredit"));
	}
	
	/*Eliminar de un cuenta*/
	@DeleteMapping("/delete/{id}")
    @ResponseStatus(code = HttpStatus.NO_CONTENT)
	public Mono<Void> deletetransaction(@PathVariable String id){
		return transacservice.deletetransaction(id);
	}
	
	
	
	/*Busqueda de todas las transacciones*/
	@GetMapping("/find")
	public Flux<Transaction> findtransaction(){
	      return transacservice.findtransaction();
    }
	
	/*Busqueda de transacciones por id*/
	@GetMapping("/find/{id}")
	public Mono<Transaction> findtransactionbyid(@PathVariable String id){
	      return transacservice.findtransactionbyid(id);
    }
	
	/*Busqueda de transacciones por fecha*/
	@GetMapping("/findbytitular/{titular}") 
	public Flux<Transaction> findtitulartransaction(@PathVariable String titular,
		  @RequestParam(name = "date1", defaultValue ="01/01/1980" )@DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate  date1,
  	      @RequestParam(name = "date2", defaultValue = "01/01/4000") @DateTimeFormat(pattern = "yyyy-MM-dd")LocalDate date2){
	      return transacservice.findclienttransaction(titular, date1, date2);
    }
	
	/*Actualizar una transaccion*/
    @PutMapping("/updatetransaction")
    @ResponseStatus(code = HttpStatus.CREATED)
    public Mono<Transaction> updatetransaction(@RequestBody Updatetransactionreq updatetransactionreq) {
      return transacservice.updatetransaction(updatetransactionreq);
    }
 
    /*Revisar un consumo vencido*/
    @GetMapping("/checkexpired/{titular}")
    public Mono<Boolean> checkforexpiredcredit(@PathVariable String titular) {
        return transacservice.checkforexpiredcredit(titular);
    }
    
    
    //Depositos por atm
    @PostMapping("/depositatm")
	public  Mono<TransactionResponse> depositatm(@RequestBody AtmtransactDto mwithdrawrequest){  
    	Mono<AccountDto> accountReq = WebClient.create(URL_ACCOUNT + "/findacc/"+mwithdrawrequest.getProductid())
                .get().retrieve().bodyToMono(AccountDto.class);  
    	return transacservice.depositatm(mwithdrawrequest,accountReq, WebClient.create(URL_ACCOUNT + "/updateaccount"));
	}
    
    //Retiros por atm
    @PostMapping("/withdrawatm")
   	public  Mono<TransactionResponse> withdrawatm(@RequestBody AtmtransactDto mwithdrawrequest){  
       	Mono<AccountDto> accountReq = WebClient.create(URL_ACCOUNT + "/findacc/"+mwithdrawrequest.getProductid())
                   .get().retrieve().bodyToMono(AccountDto.class);  
       	return transacservice.withdrawatm(mwithdrawrequest,accountReq, WebClient.create(URL_ACCOUNT + "/updateaccount"));
   	}
}
