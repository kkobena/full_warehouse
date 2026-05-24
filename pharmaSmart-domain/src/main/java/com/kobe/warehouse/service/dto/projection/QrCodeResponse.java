package com.kobe.warehouse.service.dto.projection;

public interface QrCodeResponse {
    String getCode();
    byte[] getQrCode();
}
