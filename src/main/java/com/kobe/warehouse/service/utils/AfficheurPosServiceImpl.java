package com.kobe.warehouse.service.utils;

import com.fazecast.jSerialComm.SerialPort;
import com.kobe.warehouse.service.StorageService;
import java.io.OutputStream;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional(readOnly = true)
public class AfficheurPosServiceImpl implements AfficheurPosService {

    private static final Logger LOG = LoggerFactory.getLogger(AfficheurPosServiceImpl.class);
    private final StorageService storageService;

    @Value("${port-com}")
    private String portName;

    private SerialPort serialPort;
    private OutputStream outputStream;

    public AfficheurPosServiceImpl(StorageService storageService) {
        this.storageService = storageService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        getSerialPort();
    }

    @Override
    public void sendDataToAfficheurPos(String data) {
        if (StringUtils.hasLength(portName)) {
            getSerialPort();
            try {
                data = data + " ".repeat(Math.max(0, (20 - data.length())));
                String[] dataArray = data.split("");
                for (String s : dataArray) {
                    sendData(s.charAt(0));
                }
            } catch (Exception e) {
                LOG.error("send data to afficheur pos", e);
            }
        } else {
            LOG.info("port is not open");
        }
    }

    @Override
    public void sendDataToAfficheurPos(String data, String position) {
        if (StringUtils.hasLength(portName)) {
            getSerialPort();
            try {
                String repeat = " ".repeat(Math.max(0, (20 - data.length())));
                if (position.equals("begin")) {
                    data = repeat + data;
                } else {
                    data = data + repeat;
                }
                String[] dataArray = data.split("");
                for (String s : dataArray) {
                    sendData(s.charAt(0));
                }
            } catch (Exception e) {
                LOG.error("send data to afficheur pos", e);
            }
        } else {
            LOG.info("port is not open");
        }
    }

    @Override
    public boolean isAfficheurPosEnabled() {
        return false;
    }

    @Override
    public void displaySalesData(String produitName, int qty, int price) {
        if (StringUtils.hasLength(portName)) {
            if (Objects.isNull(serialPort)) {
                getSerialPort();
            }
            if (Objects.nonNull(serialPort)) {
                sendDataToAfficheurPos(substring(produitName.toUpperCase(), 0, 20));
                sendDataToAfficheurPos(
                    substring(qty + "*" + NumberUtil.formatToString(price) + " = " + NumberUtil.formatToString(qty * price), 0, 20),
                    "begin"
                );
            }
        }
    }

    @Override
    public void welcomeMessage() {
        if (StringUtils.hasLength(portName)) {
            if (Objects.isNull(serialPort)) {
                getSerialPort();
            }

            if (Objects.nonNull(serialPort)) {
                String magasinName = storageService.getConnectedUserMagasin().getName();
                sendDataToAfficheurPos(substring(magasinName, 0, 20));
                sendDataToAfficheurPos(substring(" BIENVENUE A VOUS", 0, 20));
            }
        }
    }

    @Override
    public void connectedUserMessage(String message) {
        if (StringUtils.hasLength(portName)) {
            if (Objects.isNull(serialPort)) {
                getSerialPort();
            }

            if (Objects.nonNull(serialPort)) {
                sendDataToAfficheurPos("Caisse: " + message.toUpperCase());
            }
        }
    }

    @Override
    public void displaySaleTotal(int total) {
        if (StringUtils.hasLength(portName)) {
            if (Objects.isNull(serialPort)) {
                getSerialPort();
            }
            if (Objects.nonNull(serialPort)) {
                sendDataToAfficheurPos(substring("NET A PAYER: ", 0, 20));
                sendDataToAfficheurPos(substring(NumberUtil.formatToString(total), 0, 20), "begin");
            }
        }
    }

    @Override
    public void displayMonnaie(int total) {
        if (StringUtils.hasLength(portName)) {
            if (Objects.isNull(serialPort)) {
                getSerialPort();
            }
            if (Objects.nonNull(serialPort)) {
                sendDataToAfficheurPos(substring("MONNAIE: ", 0, 20));
                sendDataToAfficheurPos(substring(NumberUtil.formatToString(total), 0, 20), "begin");
            }
        }
    }

    private void getSerialPort() {
        try {
            if (StringUtils.hasLength(portName)) {
                serialPort = SerialPort.getCommPort(portName);
                outputStream = serialPort.getOutputStream();
                //  serialPort.setComPortParameters(9600, 8, 1, 0);
                //  serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
            }
            openSerialPort();
        } catch (Exception e) {
            LOG.error("get serial port", e);
        }
    }

    private void closeSerialPort() {
        try {
            if (Objects.nonNull(serialPort) && serialPort.isOpen()) {
                if (Objects.nonNull(outputStream)) {
                    outputStream.close();
                }
                serialPort.closePort();
            }
        } catch (Exception e) {
            LOG.error("close serial port", e);
        }
    }

    private void openSerialPort() {
        if (Objects.nonNull(serialPort) && !serialPort.isOpen()) {
            serialPort.openPort();
        }
    }

    private void sendData(char character) {
        try {
            if (Objects.nonNull(outputStream)) {
                outputStream.write(character);
            }
        } catch (Exception e) {
            LOG.error("send data to afficheur pos", e);
        }
    }

    public String substring(String s, int begin, int end) {
        if (s == null) {
            return "";
        }
        if (s.length() >= end) {
            return s.substring(begin, end);
        }
        return s;
    }
}
