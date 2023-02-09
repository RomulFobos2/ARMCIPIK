package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_water_meter")
public class WaterMeter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String factoryNumber;
    private String cabinet;

    private boolean drinkWater;
    private boolean hotWater;
    private boolean techWater;

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;


    @OneToMany(mappedBy = "waterMeter")
    private List<DataWaterMeter> allDataWaterMeters = new ArrayList<>(); //Сведения о всех показаниях

    public WaterMeter(String name, String factoryNumber, String cabinet, Department department) {
        this.name = name;
        this.factoryNumber = factoryNumber;
        this.cabinet = cabinet;
        this.department = department;
    }

    //Проверям есть ли у счетчика показания по месяцу и году
    public boolean checkDataWaterMeter(Date date) {
        for (DataWaterMeter dataWaterMeter : allDataWaterMeters) {
            if (dataWaterMeter.getDateOfDataMeter().getYear() == date.getYear()
                    & dataWaterMeter.getDateOfDataMeter().getMonth() == date.getMonth()) {
                return true;
            }
        }
        return false;
    }

    //Проверям есть ли у счетчика показания по месяцу и году на конец месяца и начало месяца
    public boolean checkDataWaterMeterFull(Date date) {
        int count = 0;
        for (DataWaterMeter dataWaterMeter : allDataWaterMeters) {
            if (dataWaterMeter.getDateOfDataMeter().getYear() == date.getYear()
                    & dataWaterMeter.getDateOfDataMeter().getMonth() == date.getMonth()) {
                count++;
            }
        }
        return count == 2 ? true : false;
    }

    //Получаем данные измерений по году, месяцу и началу или концу месяца
    public DataWaterMeter findDataWaterMeter(Date date, boolean isStartMonth) {
        DataWaterMeter result = null;
        for (DataWaterMeter dataWaterMeter : allDataWaterMeters) {
            if (dataWaterMeter.getDateOfDataMeter().getYear() == date.getYear()
                    & dataWaterMeter.getDateOfDataMeter().getMonth() == date.getMonth()
            & dataWaterMeter.isStartMonth() == isStartMonth) {
                result = dataWaterMeter;
            }
        }
        return result;
    }

    public String calcValueInMonth(Date date){
        long valueInStartMonth = findDataWaterMeter(date, true) == null ? 0 :  findDataWaterMeter(date, true).getValueMeter();
        long valueInEndMonth = findDataWaterMeter(date, false) == null ? 0 :  findDataWaterMeter(date, false).getValueMeter();
        if(valueInEndMonth == 0){
            return "Данные отсутствутют";
        }
        else {
            long result = valueInEndMonth - valueInStartMonth;
            return String.valueOf(result);
        }
    }

    public String calcAllValue(Date dateFrom, Date dateTo){
        List<DataWaterMeter> dataWaterMetersInPeriod = getAllDataWaterMeters().stream()
                .filter(x -> x.getDateOfDataMeter().getMonth() >= dateFrom.getMonth())
                .filter(x -> x.getDateOfDataMeter().getMonth() <= dateTo.getMonth())
                .filter(x -> x.getWaterMeter().checkDataWaterMeterFull(x.getDateOfDataMeter()))
                .collect(Collectors.toList());
        int valueStart = 0;
        int valueEnd = 0;
        for(DataWaterMeter dataWaterMeter : dataWaterMetersInPeriod){
            if(dataWaterMeter.isStartMonth()){
                valueStart += dataWaterMeter.getValueMeter();
            }
            else {
                valueEnd += dataWaterMeter.getValueMeter();
            }
        }
        int result = valueEnd - valueStart;
        System.out.println("result = " + result);
        return String.valueOf(result);
    }

}
