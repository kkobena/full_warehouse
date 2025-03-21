package com.kobe.warehouse.service.cash_register.dto;

import com.kobe.warehouse.domain.CashFund;
import com.kobe.warehouse.domain.CashRegister;
import com.kobe.warehouse.domain.PaymentMode;
import com.kobe.warehouse.domain.Ticketing;
import com.kobe.warehouse.domain.User;
import com.kobe.warehouse.domain.enumeration.CashRegisterStatut;
import com.kobe.warehouse.domain.enumeration.PaymentGroup;
import com.kobe.warehouse.service.dto.UserDTO;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CashRegisterDTO {

    private List<CashRegisterItemDTO> cashRegisterItems = new ArrayList<>();
    private TicketingDTO ticketing;
    private Long id;
    private UserDTO user;
    private Long initAmount;
    private Long finalAmount;
    private LocalDateTime beginTime;
    private LocalDateTime endTime;
    private LocalDateTime created;
    private LocalDateTime updated;
    private CashRegisterStatut statut;
    private int cashFund;
    private UserDTO updatedUser;
    private long estimateAmount;
    private long cashAmount;
    private long mobileAmount;
    private Integer gap;
    private int canceledAmount;

    public CashRegisterDTO(CashRegister cashRegister) {
        this.id = cashRegister.getId();
        this.user = new UserDTO(cashRegister.getUser());
        this.initAmount = cashRegister.getInitAmount();
        this.finalAmount = cashRegister.getFinalAmount();
        this.beginTime = cashRegister.getBeginTime();
        this.endTime = cashRegister.getEndTime();
        this.created = cashRegister.getCreated();
        this.updated = cashRegister.getUpdated();
        this.statut = cashRegister.getStatut();
        if (Objects.nonNull(cashRegister.getCanceledAmount())) {
            this.canceledAmount = cashRegister.getCanceledAmount();
        }

        CashFund fund = cashRegister.getCashFund();
        if (Objects.nonNull(fund)) {
            this.cashFund = fund.getAmount();
        }
        User updatedU = cashRegister.getUpdatedUser();
        if (Objects.nonNull(updatedU)) {
            this.updatedUser = new UserDTO(updatedU);
        }
        buildItems(cashRegister);
        Ticketing ticket = cashRegister.getTicketing();
        if (Objects.nonNull(ticket)) {
            this.ticketing = new TicketingDTO(
                ticket.getId(),
                cashRegister.getId(),
                ticket.getNumberOf10Thousand(),
                ticket.getNumberOf5Thousand(),
                ticket.getNumberOf2Thousand(),
                ticket.getNumberOf1Thousand(),
                ticket.getNumberOf500Hundred(),
                ticket.getNumberOf200Hundred(),
                ticket.getNumberOf100Hundred(),
                ticket.getNumberOf50(),
                ticket.getNumberOf25(),
                ticket.getNumberOf10(),
                ticket.getNumberOf5(),
                ticket.getNumberOf1(),
                ticket.getOtherAmount(),
                ticket.getTotalAmount()
            );
            this.gap = (int) (ticket.getTotalAmount() - this.estimateAmount);
        }
        this.cashRegisterItems = cashRegister.getCashRegisterItems().stream().map(CashRegisterItemDTO::new).toList();
    }

    public List<CashRegisterItemDTO> getCashRegisterItems() {
        return cashRegisterItems;
    }

    public CashRegisterDTO setCashRegisterItems(List<CashRegisterItemDTO> cashRegisterItems) {
        this.cashRegisterItems = cashRegisterItems;
        return this;
    }

    public Long getMobileAmount() {
        return mobileAmount;
    }

    public CashRegisterDTO setMobileAmount(Long mobileAmount) {
        this.mobileAmount = mobileAmount;
        return this;
    }

    public TicketingDTO getTicketing() {
        return ticketing;
    }

    public CashRegisterDTO setTicketing(TicketingDTO ticketing) {
        this.ticketing = ticketing;
        return this;
    }

    public Long getId() {
        return id;
    }

    public CashRegisterDTO setId(Long id) {
        this.id = id;
        return this;
    }

    public UserDTO getUser() {
        return user;
    }

    public CashRegisterDTO setUser(UserDTO user) {
        this.user = user;
        return this;
    }

    public Long getInitAmount() {
        return initAmount;
    }

    public CashRegisterDTO setInitAmount(Long initAmount) {
        this.initAmount = initAmount;
        return this;
    }

    public Long getFinalAmount() {
        return finalAmount;
    }

    public CashRegisterDTO setFinalAmount(Long finalAmount) {
        this.finalAmount = finalAmount;
        return this;
    }

    public LocalDateTime getBeginTime() {
        return beginTime;
    }

    public CashRegisterDTO setBeginTime(LocalDateTime beginTime) {
        this.beginTime = beginTime;
        return this;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public CashRegisterDTO setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        return this;
    }

    public LocalDateTime getCreated() {
        return created;
    }

    public CashRegisterDTO setCreated(LocalDateTime created) {
        this.created = created;
        return this;
    }

    public LocalDateTime getUpdated() {
        return updated;
    }

    public CashRegisterDTO setUpdated(LocalDateTime updated) {
        this.updated = updated;
        return this;
    }

    public CashRegisterStatut getStatut() {
        return statut;
    }

    public CashRegisterDTO setStatut(CashRegisterStatut statut) {
        this.statut = statut;
        return this;
    }

    public int getCashFund() {
        return cashFund;
    }

    public CashRegisterDTO setCashFund(int cashFund) {
        this.cashFund = cashFund;
        return this;
    }

    public UserDTO getUpdatedUser() {
        return updatedUser;
    }

    public CashRegisterDTO setUpdatedUser(UserDTO updatedUser) {
        this.updatedUser = updatedUser;
        return this;
    }

    public long getEstimateAmount() {
        return estimateAmount;
    }

    public CashRegisterDTO setEstimateAmount(long estimateAmount) {
        this.estimateAmount = estimateAmount;
        return this;
    }

    public long getCashAmount() {
        return cashAmount;
    }

    public CashRegisterDTO setCashAmount(long cashAmount) {
        this.cashAmount = cashAmount;
        return this;
    }

    public Integer getGap() {
        return gap;
    }

    public CashRegisterDTO setGap(Integer gap) {
        this.gap = gap;
        return this;
    }

    public int getCanceledAmount() {
        return canceledAmount;
    }

    public CashRegisterDTO setCanceledAmount(int canceledAmount) {
        this.canceledAmount = canceledAmount;
        return this;
    }

    private void buildItems(CashRegister cashRegister) {
        cashRegister
            .getCashRegisterItems()
            .forEach(cashRegisterItem -> {
                PaymentMode paymentMode = cashRegisterItem.getPaymentMode();
                this.cashRegisterItems.add(new CashRegisterItemDTO(cashRegisterItem));
                long amount = Objects.requireNonNullElse(cashRegisterItem.getAmount(), 0L);
                this.estimateAmount += amount;
                PaymentGroup paymentGroup = paymentMode.getGroup();
                if (paymentGroup == PaymentGroup.MOBILE) {
                    this.mobileAmount += amount;
                }
                if (paymentGroup == PaymentGroup.CASH) {
                    this.cashAmount += amount;
                }
            });
    }
}
