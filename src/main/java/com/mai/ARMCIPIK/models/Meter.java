package com.mai.ARMCIPIK.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "t_meter")
public class Meter {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    @OneToMany(mappedBy = "meter")
    private List<DataMeter> allDataMeters = new ArrayList<>(); //Сведения о всех показаниях

    @ManyToOne
    @JoinColumn(name = "department_id")
    private Department department;

    @Transient
    private HashMap<Date, Long> allValueDataMeters = new LinkedHashMap<>(); //Сведения о всех показаниях

    public Meter(String name, Department department) {
        this.name = name;
        this.department = department;
    }

    //Метод проверят, есть ли у счетчика снятые показания за дату. Нужно для формирования тыблицы на старнице statistic.html
    public boolean chekDateDataValue(Date date) {
        date.setHours(00);
        date.setMinutes(00);
        date.setSeconds(00);
        for (DataMeter dataMeter : allDataMeters) {
            if (dataMeter.getDateOfDataMeter().getTime() / 1000 == date.getTime() / 1000) {
                return true;
            }
        }
        return false;
    }

    public long getValueDataMeter(Date date) {
        date.setHours(00);
        date.setMinutes(00);
        date.setSeconds(00);
        for (DataMeter dataMeter : allDataMeters) {
            if (dataMeter.getDateOfDataMeter().getTime() / 1000 == (date.getTime() / 1000)) {
                return dataMeter.getValueMeter();
            }
        }
//        date.setDate(date.getDate()-1);
//        if(allDataMeters.get(0).getDateOfDataMeter().getTime()/1000 > (date.getTime()/1000)){
//            System.out.println("РЕКУРСИЯ для даты " + date);
//            return getValueDataMeter(date);
//        }
        return 0;
    }


    //Метод считает разницу у всех показаний
    public void reCalculateDifferentValue() {
        System.out.println("Методя пересчета для id - " + this.id);
        System.out.println("Какие имеются измерения:");
        for (DataMeter dataMeter : allDataMeters) {
            System.out.println(dataMeter.getId() + " ; " + dataMeter.getValueMeter());
        }
        System.out.println("Пересчет");
        for (int i = 1; i < allDataMeters.size(); i++) {
            DataMeter currentDataMeter = allDataMeters.get(i);
            System.out.println(currentDataMeter.getId() + " ; " + currentDataMeter.getValueMeter());
            int prevIndex = i - 1;
            boolean havePrevValue = true;
            long prevValue = allDataMeters.get(prevIndex).getValueMeter();
            while (prevValue == 0) {
                prevIndex--;
                if (prevIndex < 0) {
                    havePrevValue = false;
                    break;
                } else {
                    prevValue = allDataMeters.get(prevIndex).getValueMeter();
                }
            }
            if (havePrevValue) {
                long differentValue = currentDataMeter.getValueMeter() - prevValue;
                System.out.println("Новое значение - " + differentValue);
                currentDataMeter.setDifferentWitPrevValue(differentValue);
            }
        }
    }

    public List<DataMeter> getAllDataMeters() {
        Collections.sort(allDataMeters, new Comparator<DataMeter>() {
            @Override
            public int compare(DataMeter o1, DataMeter o2) {
                if (o1.getDateOfDataMeter() == null) {
                    return -1;
                }
                if (o2.getDateOfDataMeter() == null) {
                    return 1;
                }
                return o1.getDateOfDataMeter().compareTo(o2.getDateOfDataMeter());
            }
        });

        return allDataMeters;
    }


    //Возвращаем значение потребленной энергии в день, если день есть диапазоне снятых показаний
    public long getValueDataMeter_V2(Date date) {
        date.setHours(00);
        date.setMinutes(00);
        date.setSeconds(00);
        //Проверяем в диапазоне ли дата
        allDataMeters = getAllDataMeters();
        Date minDate = new Date(allDataMeters.get(0).getDateOfDataMeter().getTime());
        Date maxDate = new Date(allDataMeters.get(allDataMeters.size() - 1).getDateOfDataMeter().getTime());
        if (date.getTime() > minDate.getTime() & date.getTime() < maxDate.getTime()) {
            System.out.println("Считаем потребление для даты - " + date);
            if (chekDateDataValue(date)) {
                long value = getValueDataMeter(date);
                System.out.println("Показания счетчика на эту дату = " + value);
                Date rightDate = new Date(date.getTime());
                rightDate.setDate(rightDate.getDate() + 1);
                int stepCount = 1;
                while (!chekDateDataValue(rightDate)) {
                    System.out.println("Идем вправа");
                    rightDate.setDate(rightDate.getDate() + 1);
                    stepCount++;
                }
                long result = (getValueDataMeter(rightDate) - getValueDataMeter(date)) / stepCount;
                System.out.println("Нашли значение = " + result);
                return result;
            } else {
                System.out.println("В обе стороны идем");
                Date leftDate = new Date(date.getTime());
                leftDate.setDate(leftDate.getDate() - 1);
                int stepLeftCount = 1;
                while (!chekDateDataValue(leftDate)) {
                    System.out.println("Идем влево - " + leftDate);
                    leftDate.setDate(leftDate.getDate() - 1);
                    if (leftDate.getDate() == 16) {
                        System.out.println();
                    }
                    stepLeftCount++;
                }
                Date rightDate = new Date(date.getTime());
                rightDate.setDate(rightDate.getDate() + 1);
                int stepRightCount = 1;
                while (!chekDateDataValue(rightDate)) {
                    System.out.println("Идем вправа");
                    rightDate.setDate(rightDate.getDate() + 1);
                    stepRightCount++;
                }
                System.out.println("stepLeftCount+stepRightCount=" + stepLeftCount + stepRightCount);
                long result = (getValueDataMeter(rightDate) - getValueDataMeter(leftDate)) / (stepLeftCount + stepRightCount);
                System.out.println("Нашли значение = " + result);
                return result;
            }
        }
        return 0;
    }

    //Сюда пусть приходит дополнительно строковая коллекция с годами, который есть в массиве allDataMeters
    //что бы если нужно было сделать прогно для большого кол-ва дат, поиск всег годов, вызывался не миллион раз иначе провисним
    //для пришедшей даты, будем деалть прогноз, только по предыдущим годам.
    public long getPrognoses(Date date, Set<Integer> years) {
        date.setHours(00);
        date.setMinutes(00);
        date.setSeconds(00);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        System.out.println("Пришли года:" + years);
        System.out.println(date);
        System.out.println("Считаем прогноз для даты = " + simpleDateFormat.format(date));
        Iterator<Integer> iterator = years.iterator();
        while (iterator.hasNext()) {
            int year = iterator.next();
//            if (year > date.getYear()) {
            if(year >= date.getYear()){
                iterator.remove();
            }
        }
        System.out.println("Остались года:" + years);
        int summaValue = 0; //Сумма показаний счетчиков этой даты за предыдущие года
        for (int year : years) {
            Date chekDate = new Date(date.getTime());
            chekDate.setYear(year);
            long var = getValueDataMeter_V2(chekDate);
            summaValue += var;
            System.out.println("Нашли значение - " + var);
        }
        long result = 0;
        if (years.size() > 0) {
            result = summaValue / years.size();
        } else {
            result = summaValue;
        }
        System.out.println("Среднее значение = " + result);
        return result;
    }

}
