package com.picpaysimplificado.services;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TransactionService {
    @Autowired
    private UserService userService;

    @Autowired
    private TransactionRepository repository;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private NotificationService notificationService;

    public Transaction createTransaction(TransactionDTO transaction_dto) throws Exception{
        User sender = this.userService
                .findUserById(transaction_dto.sender_id());

        User receiver = this.userService
                .findUserById(transaction_dto.receiver_id());

        userService.validateTransaction(sender, transaction_dto.amount());

//        boolean authorized = this.authorizeTransaction(sender, transaction_dto.amount());
//        if(!authorized){
//            throw new Exception("Transação não autorizada");
//        }

        Transaction transaction = new Transaction();
        transaction.setAmount(transaction_dto.amount());
        transaction.setSender(sender);
        transaction.setReceiver(receiver);
        transaction.setTimestamp(LocalDateTime.now());

        sender.setBalance(sender.getBalance().subtract(transaction_dto.amount()));
        receiver.setBalance(receiver.getBalance().add(transaction_dto.amount()));

        this.repository.save(transaction);
        this.userService.saveUser(sender);
        this.userService.saveUser(receiver);

        this.notificationService.sendNotification(sender, "Transação realizada com sucesso");
        this.notificationService.sendNotification(receiver, "Transação recebida");

        return new Transaction();
    }

    public boolean authorizeTransaction(User sender, BigDecimal amount){
        ResponseEntity<Map> authorizationReponse = restTemplate
                .getForEntity("https://run.mocky.io/v3/5794d450-d2e2-4412-8131-73d0293ac1cc", Map.class);

        if (authorizationReponse.getStatusCode() == HttpStatus.OK
                && authorizationReponse.getBody().get("message") == "Autorizado"){
            return true;
        }else{
            return false;
        }
    }
}
