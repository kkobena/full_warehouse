package com.kobe.warehouse.service.utils;

public interface AfficheurPosService {
    void sendDataToAfficheurPos(String data);

    void sendDataToAfficheurPos(String data, String position);

    boolean isAfficheurPosEnabled();

    void displaySalesData(String produitName, int qty, int price);

    void welcomeMessage();

    void connectedUserMessage(String userName);

    void displaySaleTotal(int total);

    void displayMonnaie(int total);
}
