package com.axsoluciones.facturaTelegramBot.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("HARDWARE")
public class HardwareService extends Service {

    private String hardwareName;

    public HardwareService(){}

    @Override
    public String getNameToBill() {
        return this.hardwareName;
    }

    public String getHardwareName() {
        return hardwareName;
    }

    public void setHardwareName(String hardwareName) {
        this.hardwareName = hardwareName;
    }
}
