package com.mai.ARMCIPIK.controllers.user;

import com.mai.ARMCIPIK.models.*;
import com.mai.ARMCIPIK.repo.*;
import com.mai.ARMCIPIK.service.MyService;
import com.mai.ARMCIPIK.service.TemplateCreator;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИ
@Controller
public class UserPanelWaterMeterController {
    @Autowired
    WaterMeterRepository waterMeterRepository;
    @Autowired
    DataWaterMeterRepository dataWaterMeterRepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/user/allWaterMeters")
    public String WaterMetersList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<WaterMeter> allWaterMeters = waterMeterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allWaterMeters", allWaterMeters);
        return "user/waterMeter/allWaterMeters";
    }

    @GetMapping("/user/addWaterMeter")
    public String addWaterMeter(Model model) {
        return "user/waterMeter/addWaterMeter";
    }

    @PostMapping("/user/addWaterMeter")
    public String addWaterMeter(@RequestParam String inputName, @RequestParam String inputFactoryNumber,
                                @RequestParam String inputCabinet, @RequestParam int inputCharacter,
                                Model model) throws ParseException {
        if (waterMeterRepository.findByFactoryNumber(inputFactoryNumber) != null) {
            model.addAttribute("waterMeterFactoryNumberError", "Средство измерения с таким заводским номером уже существует");
            return "user/waterMeter/addWaterMeter";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        WaterMeter waterMeter = new WaterMeter(inputName, inputFactoryNumber, inputCabinet, department);
        switch (inputCharacter) {
            case 0:
                waterMeter.setDrinkWater(true);
                break;
            case 1:
                waterMeter.setHotWater(true);
                break;
            case 2:
                waterMeter.setTechWater(true);
                break;
        }
        waterMeterRepository.save(waterMeter);
        return "redirect:/user/allWaterMeters";
    }

    @GetMapping("/user/allWaterMeters/waterMeter-details/{id}")
    public String waterMeterDetails(@PathVariable(value = "id") long id, Model model) throws IOException {
        if (!waterMeterRepository.existsById(id)) {
            return "redirect:/user/allWaterMeters";
        }
        WaterMeter waterMeter = waterMeterRepository.findById(id).orElseThrow();
        model.addAttribute("waterMeter", waterMeter);

        List<DataWaterMeter> allDataWaterMeter = waterMeter.getAllDataWaterMeters();
        model.addAttribute("allDataWaterMeter", allDataWaterMeter);

        Date dateFrom = new Date();
        Date dateTo = new Date();
        for(DataWaterMeter dataWaterMeter : allDataWaterMeter){
            if(dataWaterMeter.getDateOfDataMeter().getTime()/1000 <= (dateFrom.getTime()/1000)){
                dateFrom = dataWaterMeter.getDateOfDataMeter();
            }
            if(dataWaterMeter.getDateOfDataMeter().getTime()/1000 >= (dateTo.getTime()/1000)){
                dateTo = dataWaterMeter.getDateOfDataMeter();
            }
        }
        MyService myService = new MyService();
        model.addAttribute("myService", myService);
        List<Date> monthList = MyService.getMonthList(dateFrom, dateTo);
        model.addAttribute("monthList", monthList);


        return "user/waterMeter/waterMeter-details";
    }

    @PostMapping("/user/allWaterMeters/waterMeter-details/{id}/remove")
    public String delete_waterMeter(@PathVariable(value = "id") long id, Model model) {
        WaterMeter waterMeter = waterMeterRepository.findById(id).orElseThrow();
        List<DataWaterMeter> allDataWaterMeters = waterMeter.getAllDataWaterMeters();
        for (DataWaterMeter dataWaterMeter : allDataWaterMeters) {
            dataWaterMeterRepository.delete(dataWaterMeter);
        }
        waterMeterRepository.delete(waterMeter);
        return "redirect:/user/allWaterMeters";
    }


    @GetMapping("/user/editWaterMeter/{id}")
    public String waterMeterEdit(@PathVariable(value = "id") long id,
                                 Model model, HttpServletRequest request) {
        if (!waterMeterRepository.existsById(id)) {
            return "redirect:/user/allWaterMeters";
        }
        if (request.getSession().getAttribute("waterMeterFactoryNumberError") != null
                && (Boolean) request.getSession().getAttribute("waterMeterFactoryNumberError")) {
            model.addAttribute("waterMeterFactoryNumberError",
                    "Средство измерения с таким заводским номером уже существует");
            request.getSession().setAttribute("waterMeterFactoryNumberError", false);
        }
        WaterMeter waterMeter = waterMeterRepository.findById(id).orElseThrow();
        model.addAttribute("waterMeter", waterMeter);
        return "user/waterMeter/editWaterMeter";
    }

    @PostMapping("/user/editWaterMeter/{id}")
    public String waterMeterEdit(@PathVariable(value = "id") long id,
                                 @RequestParam String inputName, @RequestParam String inputFactoryNumber,
                                 @RequestParam String inputCabinet, @RequestParam int inputCharacter,
                                 Model model, HttpServletRequest request) {
        WaterMeter waterMeter = waterMeterRepository.findById(id).orElseThrow();
        if (waterMeterRepository.findByFactoryNumber(inputFactoryNumber) != null && waterMeterRepository.findByFactoryNumber(inputFactoryNumber).getId() != id) {
            request.getSession().setAttribute("waterMeterFactoryNumberError", true);
            return "redirect:/user/editWaterMeter/{id}";
        }

        switch (inputCharacter) {
            case 0:
                waterMeter.setDrinkWater(true);
                break;
            case 1:
                waterMeter.setHotWater(true);
                break;
            case 2:
                waterMeter.setTechWater(true);
                break;
        }
        waterMeter.setName(inputName);
        waterMeter.setFactoryNumber(inputFactoryNumber);
        waterMeter.setCabinet(inputCabinet);
        waterMeterRepository.save(waterMeter);
        return "redirect:/user/allWaterMeters/waterMeter-details/{id}";
    }

    @GetMapping("/user/allCurrentDataWaterMeters")
    public String allCurrentDataMetersList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        Date today = new Date();
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);
        //Берем только те показания, которые созданы для сегодняшнего числа
        //List<DataMeter> allDataMeters = dataMeterRepository.findAll().stream().filter(x -> x.getDateOfDataMeter().getTime() / 1000 >= (today.getTime() / 1000)).filter(x -> x.getValueMeter().equals("")).collect(Collectors.toList());
        final boolean isStartMonth = (today.getDate() < 15) ? true : false;

        List<DataWaterMeter> allDataWaterMeters = dataWaterMeterRepository.findAll().stream()
                .filter(x -> x.getWaterMeter().getDepartment().getId() == department.getId())
                .filter(x -> x.isStartMonth() == isStartMonth)
                .filter(x -> x.getDateOfDataMeter().getMonth() == today.getMonth())
                .collect(Collectors.toList());
        model.addAttribute("allDataWaterMeters", allDataWaterMeters);
        model.addAttribute("isStartMonth", isStartMonth);
        return "user/waterMeter/allCurrentDataWaterMeters";
    }

    @PostMapping("/user/allCurrentDataWaterMeters")
    public String allCurrentDataWaterMetersList(@RequestParam List<Long> dataWaterMeterId,
                                                @RequestParam List<Long> inputValueWaterMeter, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        for (int i = 0; i < dataWaterMeterId.size(); i++) {
            DataWaterMeter dataWaterMeter = dataWaterMeterRepository.findById(dataWaterMeterId.get(i)).orElseThrow();
            dataWaterMeter.setValueMeter(inputValueWaterMeter.get(i));
            dataWaterMeter.setUser(user);
            dataWaterMeterRepository.save(dataWaterMeter);
            waterMeterRepository.save(dataWaterMeter.getWaterMeter());
        }
        return "redirect:/user/allWaterMeters";
    }


    @GetMapping("/user/allCurrentDataWaterMetersHand")
    public String allCurrentDataMetersHandList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();


        List<WaterMeter> allWaterMeters = waterMeterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allWaterMeters", allWaterMeters);
        return "user/waterMeter/allCurrentDataWaterMetersHand";
    }

    @PostMapping("/user/allCurrentDataWaterMetersHand")
    public String allCurrentDataWaterMetersHandList(@RequestParam(required = false) List<String> inputValueDate,
                                                    @RequestParam(required = false)  List<Long> waterMeterId,
                                                    @RequestParam(required = false)  List<Long> inputValueWaterMeter, Model model) throws ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        SimpleDateFormat simpleDateFormat_In = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        if(inputValueDate != null){
            for(int i = 0; i < inputValueDate.size(); i++){
                Date date = simpleDateFormat_In.parse(inputValueDate.get(i));
                final boolean isStartMonth = (date.getDate() < 15) ? true : false;
                WaterMeter waterMeter = waterMeterRepository.findById(waterMeterId.get(i)).orElseThrow();
                //Если у счетчика уже есть измерение за текущий месяц и начало/конц года, то меняем его
                //иначе, создаем новый и вписываем значение туда
                DataWaterMeter dataWaterMeter = waterMeter.findDataWaterMeter(date, isStartMonth);
                if(dataWaterMeter != null){
                    dataWaterMeter.setValueMeter(inputValueWaterMeter.get(i));
                    dataWaterMeter.setUser(user);
                    dataWaterMeterRepository.save(dataWaterMeter);
                    waterMeterRepository.save(waterMeter);
                }
                else{
                    DataWaterMeter new_dataWaterMeter = new DataWaterMeter(date, waterMeter, isStartMonth);
                    new_dataWaterMeter.setValueMeter(inputValueWaterMeter.get(i));
                    new_dataWaterMeter.setUser(user);
                    dataWaterMeterRepository.save(new_dataWaterMeter);
                    waterMeterRepository.save(waterMeter);
                }
            }
        }
        return "redirect:/user/allWaterMeters";
    }


    @PostMapping("/user/allWaterMeters/statistics")
    public String statistics(@RequestParam String resultFromInput, @RequestParam String resultToInput,
                             Model model) throws ParseException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_PeriodInner = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat simpleDateFormat_PeriodOut = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Date dateFrom = simpleDateFormat_PeriodInner.parse(resultFromInput);
        Date dateTo = simpleDateFormat_PeriodInner.parse(resultToInput);
        //таким образом, в дате dateTo всегда получаем последний день месяца
        //И т.о. у нас период будет с 1 числа dateFrom по 31/30/28/29 число dateTo
        dateTo.setMonth(dateTo.getMonth() + 1);
        dateTo.setDate(dateTo.getDate() - 1);
        dateTo.setHours(23);
        dateTo.setMinutes(59);
        String strDateFrom = simpleDateFormat_PeriodOut.format(dateFrom);
        String strDateTo = simpleDateFormat_PeriodOut.format(dateTo);
        System.out.println("strDateFrom - " + strDateFrom);
        System.out.println("strDateTo - " + strDateTo);

        List<WaterMeter> allWaterMeters = waterMeterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());


        model.addAttribute("allWaterMeters", allWaterMeters);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        MyService myService = new MyService();
        model.addAttribute("myService", myService);

        List<Date> monthList = MyService.getMonthList(dateFrom, dateTo);
        model.addAttribute("monthList", monthList);


        long summaValueForDrinkMeter = 0;
        long summaValueForHotMeter = 0;
        long summaValueForTechMeter = 0;

        for(WaterMeter waterMeter : allWaterMeters){
            if(waterMeter.isDrinkWater()){
                summaValueForDrinkMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
            if(waterMeter.isHotWater()){
                summaValueForHotMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
            if(waterMeter.isTechWater()){
                summaValueForTechMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
        }
        long summaValueAllMeter = summaValueForDrinkMeter + summaValueForHotMeter + summaValueForTechMeter;

        model.addAttribute("summaValueForDrinkMeter", summaValueForDrinkMeter);
        model.addAttribute("summaValueForHotMeter", summaValueForHotMeter);
        model.addAttribute("summaValueForTechMeter", summaValueForTechMeter);
        model.addAttribute("summaValueAllMeter", summaValueAllMeter);
        return "user/waterMeter/statistics";
    }


    @PostMapping("/user/allWaterMeters/getReport")
    public ResponseEntity<Object> waterMeterReport(@RequestParam String resultFromInput, @RequestParam String resultToInput) throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        String userFullName = user.getFullName();
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");
        SimpleDateFormat simpleDateFormat_PeriodInner = new SimpleDateFormat("yyyy-MM", Locale.getDefault());
        SimpleDateFormat simpleDateFormat_PeriodOut = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());


        Date today = new Date();
        Date dateFrom = simpleDateFormat_PeriodInner.parse(resultFromInput);
        Date dateTo = simpleDateFormat_PeriodInner.parse(resultToInput);
        //таким образом, в дате dateTo всегда получаем последний день месяца
        //И т.о. у нас период будет с 1 числа dateFrom по 31/30/28/29 число dateTo
        dateTo.setMonth(dateTo.getMonth() + 1);
        dateTo.setDate(dateTo.getDate() - 1);
        dateTo.setHours(23);
        dateTo.setMinutes(59);
        String strToday = simpleDateFormat_today.format(today);
        String strDateFrom = simpleDateFormat_PeriodOut.format(dateFrom);
        String strDateTo = simpleDateFormat_PeriodOut.format(dateTo);


        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_1.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, strDateFrom, "dateFrom");
        TemplateCreator.replacePlaceholder(wordDocument, strDateTo, "dateTo");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");


        List<WaterMeter> allWaterMeters = waterMeterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        long summaValueForDrinkMeter = 0;
        long summaValueForHotMeter = 0;
        long summaValueForTechMeter = 0;
        for(WaterMeter waterMeter : allWaterMeters){
            if(waterMeter.isDrinkWater()){
                summaValueForDrinkMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
            if(waterMeter.isHotWater()){
                summaValueForHotMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
            if(waterMeter.isTechWater()){
                summaValueForTechMeter += Long.parseLong(waterMeter.calcAllValue(dateFrom, dateTo));
            }
        }

        System.out.println("summaValueForDrinkMeter = " + summaValueForDrinkMeter);
        System.out.println("summaValueForHotMeter = " + summaValueForHotMeter);
        System.out.println("summaValueForTechMeter = " + summaValueForTechMeter);

        long summaValueAllMeter = summaValueForDrinkMeter + summaValueForHotMeter + summaValueForTechMeter;
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaValueForDrinkMeter), "summaDrink");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaValueForHotMeter), "summaHot");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaValueForTechMeter), "summaTech");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaValueAllMeter), "summaAll");

        List<Date> monthList = MyService.getMonthList(dateFrom, dateTo);

        TemplateCreator.addWaterMeterRow(wordDocument, allWaterMeters, "num", monthList);

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }

}
