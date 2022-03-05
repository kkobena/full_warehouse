package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Logs;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.LogsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogsService {
    private final LogsRepository logsRepository;
    private final UserService userService;

    public LogsService(LogsRepository logsRepository, UserService userService) {
        this.logsRepository = logsRepository;
        this.userService = userService;
    }
    public  void create(TransactionType transactionType,String comments,String indentityKey){
        Logs logs=new Logs();
        logs.setComments(comments);
        logs.setIndentityKey(indentityKey);
        logs.setTransactionType(transactionType);
        logs.setUser(userService.getUser());
        logsRepository.save(logs);
    }
}
