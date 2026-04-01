package com.axsoluciones.facturaTelegramBot.entity;

import com.axsoluciones.facturaTelegramBot.entity.Service;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.time.LocalDate;

@Entity
@DiscriminatorValue("SOFTWARE")
public class SoftwareService extends Service {
    private LocalDate expiredDate;
    private String softwareName;

    public SoftwareService(){}

    @Override
    public String getNameToBill() {
        return this.softwareName;
    }

    public void setExpiredDate(LocalDate expiredDate) {
        this.expiredDate = expiredDate;
    }

    public void setSoftwareName(String softwareName) {
        this.softwareName = softwareName;
    }

    public LocalDate getExpiredDate() {
        return expiredDate;
    }

    public String getSoftwareName() {
        return softwareName;
    }
}
