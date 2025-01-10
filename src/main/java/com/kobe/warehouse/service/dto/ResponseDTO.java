package com.kobe.warehouse.service.dto;

import java.util.List;

public class ResponseDTO {

    private String message, errorCode;
    private boolean success, completed;
    private int totalSize;
    private int size;
    private List<?> data;
    private String rejectFileUrl;
    private int errorSize;

    public ResponseDTO() {
        super();
    }

    public int getErrorSize() {
        return errorSize;
    }

    public ResponseDTO setErrorSize(int errorSize) {
        this.errorSize = errorSize;
        return this;
    }

    public String getRejectFileUrl() {
        return rejectFileUrl;
    }

    public ResponseDTO setRejectFileUrl(String rejectFileUrl) {
        this.rejectFileUrl = rejectFileUrl;
        return this;
    }

    public ResponseDTO size(List<?> data) {
        this.data = data;
        return this;
    }

    public ResponseDTO size(int size) {
        this.size = size;
        return this;
    }

    public ResponseDTO totalSize(int totalSize) {
        this.totalSize = totalSize;
        return this;
    }

    public boolean isCompleted() {
        return completed;
    }

    public ResponseDTO setCompleted(boolean completed) {
        this.completed = completed;
        return this;
    }

    public ResponseDTO success(boolean success) {
        this.success = success;
        return this;
    }

    public ResponseDTO errorCode(String errorCode) {
        this.errorCode = errorCode;
        return this;
    }

    public ResponseDTO message(String message) {
        this.message = message;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getTotalSize() {
        return totalSize;
    }

    public void setTotalSize(int totalSize) {
        this.totalSize = totalSize;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public List<?> getData() {
        return data;
    }

    public ResponseDTO setData(List<?> data) {
        this.data = data;
        return this;
    }

    @Override
    public String toString() {
        return (
            "ResponseDTO [message=" +
            message +
            ", errorCode=" +
            errorCode +
            ", success=" +
            success +
            ", totalSize=" +
            totalSize +
            ", size=" +
            size +
            ", data=" +
            data +
            "]"
        );
    }
}
