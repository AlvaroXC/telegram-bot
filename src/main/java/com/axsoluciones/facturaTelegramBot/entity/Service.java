package com.axsoluciones.facturaTelegramBot.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "service_type")
public abstract class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    private Double price;
    private LocalDate hiringDate;

    private LocalDate invoiceReminderDate;

    @Enumerated(EnumType.STRING)
    private BillState billState;
    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    public Service(){}

    public abstract String getNameToBill();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public LocalDate getHiringDate() {
        return hiringDate;
    }

    public void setHiringDate(LocalDate hiringDate) {
        this.hiringDate = hiringDate;
    }

    public LocalDate getInvoiceReminderDate() {
        return invoiceReminderDate;
    }

    public void setInvoiceReminderDate(LocalDate invoiceReminderDate) {
        this.invoiceReminderDate = invoiceReminderDate;
    }

    public BillState getBillState() {
        return billState;
    }

    public void setBillState(BillState billState) {
        this.billState = billState;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

}
