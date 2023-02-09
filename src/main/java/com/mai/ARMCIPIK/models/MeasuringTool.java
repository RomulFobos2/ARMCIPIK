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
@Table(name = "t_measuring_tool")

public class MeasuringTool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String officeName;//Наименование филиала
    private String departmentName;//Наименование (номер) эксплуатирующего подразделения филиала
    private String measuringToolName;//Наименование и тип СИ
    private String factoryNumber;//Заводской номер СИ
    private String measuringRange;//Диапазон измерений СИ
    private String accuracyClass;//Класс точности
    private String registrationNumberStateRegister;//Регистрационный номер типа СИ в Госреестре
    private int countVerification;//Периодичность поверки СИ
    private boolean scopeOfApplication;//Сфера применения СИ, (СГОЕИ/нет)
    private boolean reducedVerification;//Сокращенная поверка СИ,  (СП/нет)
    private boolean checkingGRCM;//Поверка СИ в ГРЦМ, (ГРМЦ/нет)

    @OneToMany(mappedBy = "measuringTool")
    private List<Verification> verifications; //Сведения о всех поверках

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;


    public MeasuringTool(String officeName, String departmentName,
                         String measuringToolName, String factoryNumber,
                         String measuringRange, String accuracyClass,
                         String registrationNumberStateRegister, int countVerification,
                         boolean scopeOfApplication, boolean reducedVerification,
                         boolean checkingGRCM, List<Verification> verifications,
                         Department department) {
        this.officeName = officeName;
        this.departmentName = departmentName;
        this.measuringToolName = measuringToolName;
        this.factoryNumber = factoryNumber;
        this.measuringRange = measuringRange;
        this.accuracyClass = accuracyClass;
        this.registrationNumberStateRegister = registrationNumberStateRegister;
        this.countVerification = countVerification;
        this.scopeOfApplication = scopeOfApplication;
        this.reducedVerification = reducedVerification;
        this.checkingGRCM = checkingGRCM;
        this.verifications = verifications;
        this.department = department;
    }
}
