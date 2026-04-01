package com.axsoluciones.facturaTelegramBot.entity;

import com.axsoluciones.facturaTelegramBot.entity.Service;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("MANTENIMIENTO")
public class MaintenanceService extends Service {

    @Column(columnDefinition = "TEXT")
    private String description;


    public MaintenanceService(){}

    @Override
    public String getNameToBill() {
        return this.description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
