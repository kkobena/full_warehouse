package com.kobe.warehouse.service;

import com.kobe.warehouse.domain.Logs;
import com.kobe.warehouse.domain.Produit;
import com.kobe.warehouse.domain.enumeration.TransactionType;
import com.kobe.warehouse.repository.LogsRepository;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class LogsService {
  private final LogsRepository logsRepository;
  private final UserService userService;
  private final MessageSource messageSource;

  public LogsService(
      LogsRepository logsRepository, UserService userService, MessageSource messageSource) {
    this.logsRepository = logsRepository;
    this.userService = userService;
    this.messageSource = messageSource;
  }

  public void create(TransactionType transactionType, String comments, String indentityKey) {
    Logs logs = new Logs();
    logs.setComments(comments);
    logs.setIndentityKey(indentityKey);
    logs.setTransactionType(transactionType);
    logs.setUser(userService.getUser());
    logsRepository.save(logs);
  }

  public void create(
      TransactionType transactionType,
      String messageKey,
      Object[] messageArgs,
      String indentityKey) {
    Logs logs = new Logs();
    logs.setComments(getMessageByKey(messageKey, messageArgs));
    logs.setIndentityKey(indentityKey);
    logs.setTransactionType(transactionType);
    logs.setUser(userService.getUser());
    logsRepository.save(logs);
  }

  public String getMessageByKey(String key, Object[] args) {

    return messageSource.getMessage(key, args, null);
  }

  public void create(
      TransactionType transactionType,
      String comments,
      String indentityKey,
      String old,
      String newObject) {
    Logs logs = new Logs();
    logs.setComments(comments);
    logs.setIndentityKey(indentityKey);
    logs.setTransactionType(transactionType);
    logs.setUser(userService.getUser());
    logs.setNewObject(newObject);
    logs.setOldObject(old);
    logsRepository.save(logs);
  }
    public void create(TransactionType transactionType, String comments, String indentityKey,
        Produit produit) {
        Logs logs = new Logs();
        logs.setComments(comments);
        logs.setIndentityKey(indentityKey);
        logs.setTransactionType(transactionType);
        logs.setUser(userService.getUser());
        logsRepository.save(logs);
    }
}
