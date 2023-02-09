package com.mai.ARMCIPIK.service;

import com.mai.ARMCIPIK.models.*;
import com.mai.ARMCIPIK.repo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class ScheduleTask {
    @Autowired
    private MailSender mailSender;
    @Autowired
    private MeasuringToolRepository measuringToolRepository;
    @Autowired
    private VerificationRepository verificationRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeterRepository meterRepository;
    @Autowired
    private DataMeterRepository dataMeterRepository;
    @Autowired
    private WaterMeterRepository waterMeterRepository;
    @Autowired
    private DataWaterMeterRepository dataWaterMeterRepository;

    //При запуске, а затем через каждые 600 секунд (для демонстрации оставим пока каждые 5 секунд)
    //Оповещения о предстоящих поверках СИ
    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void sendMessage() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");
        List<MeasuringTool> allMeasuringTools = measuringToolRepository.findAll();
        List<Verification> allLastVerifications = new ArrayList<>();
        for (MeasuringTool measuringTool : allMeasuringTools) {
            Verification lastVerification = measuringTool.getVerifications().get(measuringTool.getVerifications().size() - 1);
            if (lastVerification.isCheckVerification()) {
                allLastVerifications.add(lastVerification);
            }
        }
        LocalDate today = LocalDate.now();
        String textMessage = "";
        boolean needSendLetter = false;
        for (Verification lastVerification : allLastVerifications) {
            Date nextVerification = lastVerification.getNextVerificationDate();
            Period period = Period.between(today, nextVerification.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
            if (period.getYears() == 0 & period.getMonths() == 0 & period.getDays() <= 7) {
                needSendLetter = true;
                String messageAboutVerification = "Наименование и тип СИ: " + lastVerification.getMeasuringTool().getMeasuringToolName()
                        + ".<br>Заводской номер: " + lastVerification.getMeasuringTool().getFactoryNumber() + ".<br>Дата поверки: " + dateFormat.format(lastVerification.getNextVerificationDate());
                Verification newVerification = new Verification(lastVerification.getMeasuringTool());
                verificationRepository.save(newVerification);
                textMessage += "<br>" + HtmlLetter.getLetterPart(messageAboutVerification);
            }
        }

        if (needSendLetter) {
            List<User> allUsers = userRepository.findAll().stream()
                    .filter(x -> x.getRoles()
                            .stream()
                            .filter(y -> y.getName().equals("ROLE_USER")).collect(Collectors.toList()).size() != 0).collect(Collectors.toList());
            if (allUsers.size() != 0) {
                User user = allUsers.get(0);
                try {
                    mailSender.sendHTML(user.getUsername(), "Оповещение о предстоящих поверках", HtmlLetter.getLetterResult(textMessage));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //Добавляем каждому счетчику электроэнергии данные для заполнения, если на сегодня нет уже созданной такой записи
    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void checkDataMeters() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE");
        List<Meter> allMeters = meterRepository.findAll();
        for (Meter meter : allMeters) {
            Date today = new Date();
            today.setHours(0);
            today.setMinutes(0);
            today.setSeconds(0);
            //обнуляем кончики милисекунд, что в БД записалось корректная дата 00часов 00минут 00секунду (без обнуления кончика мс, записывается 01секунда)
            today.setTime(today.getTime()/1000);
            today.setTime(today.getTime()*1000);
            //НЕЗАБЫВАМ ПРИ СРАВНЕНИИ ОТСЕЧЬ МИЛЛИСЕКУНДЫ (ПРОСТО ДЕЛИМ НА 1000)
            List<DataMeter> allDataMeters = meter.getAllDataMeters().stream().filter(x -> x.getDateOfDataMeter().getTime()/1000 >= (today.getTime()/1000)).collect(Collectors.toList());
            if (allDataMeters.size() == 0) {
                int indexDay = MyService.getDayOfWeek(dateFormat.format(today));
                DataMeter todayDataMeter = new DataMeter(today, indexDay, dateFormat.format(today), meter);
                dataMeterRepository.save(todayDataMeter);
            }
        }
    }


    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void checkDataWaterMeters() {
        List<WaterMeter> allWaterMeters = waterMeterRepository.findAll();
        Date today = new Date();
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);
        //обнуляем кончики милисекунд, что в БД записалось корректная дата 00часов 00минут 00секунду (без обнуления кончика мс, записывается 01секунда)
        today.setTime(today.getTime()/1000);
        today.setTime(today.getTime()*1000);

        //Начало или конец месяца?
        final boolean isStartMonth = (today.getDate() < 15) ? true : false;

        for(WaterMeter waterMeter : allWaterMeters){
            //Беру все данные счетчика, оставляю только те который либо конец месяца, либо начала
            //затем те у кого месяц равен сегодняшнему, если в итоге размер коллекции == 0, то создаю запись
            List<DataWaterMeter> allDataWaterMeters = waterMeter.getAllDataWaterMeters().stream()
                    .filter(x -> x.isStartMonth() == isStartMonth)
                    .filter(x -> x.getDateOfDataMeter().getMonth() == today.getMonth())
                    .collect(Collectors.toList());
            if(allDataWaterMeters.size() == 0){
                DataWaterMeter dataWaterMeter = new DataWaterMeter(today, waterMeter, isStartMonth);
                dataWaterMeterRepository.save(dataWaterMeter);
            }
        }

    }
}
