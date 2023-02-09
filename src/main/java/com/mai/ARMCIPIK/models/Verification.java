package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_verification")

public class Verification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date verificationDate;//Дата поверки СИ Verification
    private String verifyingOrganization;//Организация-поверитель, выполнившая поверку СИ
    private String certificateNumber;//Номер свидетельства/извещения СИ
    private boolean availability;//Пригодность СИ (пригоден/не пригоден)
    @DateTimeFormat(pattern = "dd-MM-yyyy")
    private Date nextVerificationDate;//Дата следующей поверки СИ
    private String basisForDecommissioning;//Основание для вывода из эксплуатации СИ (в поверке/в ремонте)
    private boolean checkVerification; //Признак того, что поверка пройдена (true) или нет (false)

    @ManyToOne
    private MeasuringTool measuringTool;

    public Verification(Date verificationDate, String verifyingOrganization,
                        String certificateNumber, boolean availability,
                        Date nextVerificationDate, String basisForDecommissioning, boolean checkVerification) {
        this.verificationDate = verificationDate;
        this.verifyingOrganization = verifyingOrganization;
        this.certificateNumber = certificateNumber;
        this.availability = availability;
        this.nextVerificationDate = nextVerificationDate;
        this.basisForDecommissioning = basisForDecommissioning;
        this.checkVerification = checkVerification;
    }

    public Verification(MeasuringTool measuringTool) {
        this.measuringTool = measuringTool;
    }
}
