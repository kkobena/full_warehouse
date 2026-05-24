package com.kobe.warehouse.service.utils;

public interface CustomerDisplayService {
    void sendDataToAfficheurPos(String data);

    void sendDataToAfficheurPos(String data, String position);

    void displaySalesData(String produitName, int qty, int price);

    void welcomeMessage();

    void connectedUserMessage(String userName);

    void displaySaleTotal(int total);

    void displayMonnaie(int total);
}
