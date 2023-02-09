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
import javax.servlet.http.HttpServletRequestWrapper;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИ
@Controller
public class UserPanelWeekRequestController {
    @Autowired
    private WeekRequestRepository weekRequestRepository;
    @Autowired
    private ValueForWeekRepository valueForWeekRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private MeterRepository meterRepository;
    @Autowired
    private DataMeterRepository dataMeterRepository;

    @GetMapping("/user/allWeekRequests")
    public String weekRequestsList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        List<WeekRequest> allWeekRequests = weekRequestRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allWeekRequests", allWeekRequests);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        Set<String> years = new TreeSet<>();
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            Date date = new Date(dataMeter.getDateOfDataMeter().getTime());
            years.add(simpleDateFormat.format(date));
        }


        System.out.println(years);
        model.addAttribute("years", years);


        return "user/weekRequest/allWeekRequests";
    }


    @GetMapping("/user/addWeekRequest/firstStep")
    public String addWeekRequestFirstStep(Model model) {
        List<String> listTimeForWeekRequest = MyService.listTimeForWeekRequest();
        return "user/weekRequest/addWeekRequestFirstStep";
    }

    @PostMapping("/user/addWeekRequest/firstStep")
    public String addWeekRequestSecondStep(@RequestParam String inputWeek, @RequestParam Integer inputValue,
                                           Model model, HttpServletRequest request) throws ParseException, IOException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-'W'ww");
        System.out.println(inputWeek);
        int numberWeek = Integer.parseInt(inputWeek.split("W")[1]);
        System.out.println("numberWeek = " + numberWeek);

        SimpleDateFormat simpleDateFormat_session = new SimpleDateFormat("dd.MM.yyyy");
        Date dateFrom = simpleDateFormat.parse(inputWeek);
        Date dateTo = new Date(dateFrom.getTime());
        dateTo.setDate(dateTo.getDate() + 6);
        dateTo.setHours(23);
        dateTo.setMinutes(59);
        dateTo.setSeconds(59);

        MyService myService = new MyService();

        List<String> listTimeForWeekRequest = MyService.listTimeForWeekRequest();
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        Set<Integer> yearsDataMeter = new TreeSet<>();
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            yearsDataMeter.add(dataMeter.getDateOfDataMeter().getYear());
        }

        long allEnergyInWeek = 0;

        int countWorkDay = 0;
        int countWorkOffDay = 0;
        Date varDate = new Date(dateFrom.getTime());
        while (varDate.getTime() / 1000 <= dateTo.getTime() / 1000) {
            if (MyService.isDayOff(varDate) == 0) {
                countWorkDay++;
            } else {
                countWorkOffDay++;
            }

            for (Meter meter : allMeters) {
                allEnergyInWeek += meter.getPrognoses(varDate, yearsDataMeter);
            }
            varDate.setDate(varDate.getDate() + 1);
        }

        System.out.println("На всю неделю = " + allEnergyInWeek);

        long energyInWorkHour = (allEnergyInWeek - (inputValue * countWorkOffDay * 24) - (inputValue * countWorkDay * 16)) / (countWorkDay * 8);
        long mysumma = countWorkDay * 8 * energyInWorkHour + inputValue * countWorkOffDay * 24 + inputValue * countWorkDay * 16;
        System.out.println("На всю неделю подсчет = " + mysumma);
        System.out.println("Отсаток от прогноза = " + (allEnergyInWeek - mysumma));

        model.addAttribute("allEnergyInWeek", allEnergyInWeek);
        model.addAttribute("mysumma", mysumma);


        request.getSession().setAttribute("dateFrom", simpleDateFormat_session.format(dateFrom));
        request.getSession().setAttribute("dateTo", simpleDateFormat_session.format(dateTo));
        request.getSession().setAttribute("countWorkDay", countWorkDay);
        request.getSession().setAttribute("countWorkOffDay", countWorkOffDay);
        request.getSession().setAttribute("numberWeek", numberWeek);

        model.addAttribute("listTimeForWeekRequest", listTimeForWeekRequest);
        model.addAttribute("department", department);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        model.addAttribute("myService", myService);
        model.addAttribute("inputValue", inputValue);
        model.addAttribute("countWorkDay", countWorkDay);
        model.addAttribute("countWorkOffDay", countWorkOffDay);
        model.addAttribute("energyInWorkHour", energyInWorkHour);
        return "user/weekRequest/addWeekRequestSecondStep";
    }


    @PostMapping("/user/addWeekRequest/secondStep")
    public String addWeekRequestSecondStep(@RequestParam List<Integer> inputValue, @RequestParam List<Integer> inputValueWorkOff,
                                           Model model, HttpServletRequest request) throws ParseException, IOException, CloneNotSupportedException, JAXBException, Docx4JException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_session = new SimpleDateFormat("dd.MM.yyyy");
        Date dateFrom = simpleDateFormat_session.parse((String) request.getSession().getAttribute("dateFrom"));
        Date dateTo = simpleDateFormat_session.parse((String) request.getSession().getAttribute("dateTo"));
        Date dateCreate = new Date();
        int countWorkDay = (int) request.getSession().getAttribute("countWorkDay");
        int countWorkOffDay = (int) request.getSession().getAttribute("countWorkOffDay");
        int numberWeek = (int) request.getSession().getAttribute("numberWeek");
        WeekRequest weekRequest = new WeekRequest(dateCreate, dateFrom, dateTo, countWorkDay, countWorkOffDay, numberWeek, department, user);
        weekRequestRepository.save(weekRequest);
        List<String> listTimeForWeekRequest = MyService.listTimeForWeekRequest();
        for (int i = 0; i < 24; i++) {
            ValueForWeek valueForWeek = new ValueForWeek(listTimeForWeekRequest.get(i), inputValue.get(i), true, weekRequest);
            valueForWeekRepository.save(valueForWeek);
        }
        for (int i = 0; i < 24; i++) {
            ValueForWeek valueForWeek = new ValueForWeek(listTimeForWeekRequest.get(i), inputValueWorkOff.get(i), false, weekRequest);
            valueForWeekRepository.save(valueForWeek);
        }
        return "redirect:/user/allWeekRequests";
    }


    @GetMapping("/user/allWeekRequests/weekRequest-details/{id}")
    public String weekRequestDetails(@PathVariable(value = "id") long id, Model model) {
        if (!weekRequestRepository.existsById(id)) {
            return "redirect:/user/allWeekRequests";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        WeekRequest weekRequest = weekRequestRepository.findById(id).orElseThrow();
        model.addAttribute("weekRequest", weekRequest);
        List<ValueForWeek> allValueForWeeksWD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> x.isWorkDay())
                .collect(Collectors.toList());
        List<ValueForWeek> allValueForWeeksWOffD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> !x.isWorkDay())
                .collect(Collectors.toList());
        MyService myService = new MyService();
        model.addAttribute("allValueForWeeksWD", allValueForWeeksWD);
        model.addAttribute("allValueForWeeksWOffD", allValueForWeeksWOffD);
        model.addAttribute("department", department);
        model.addAttribute("myService", myService);
        return "user/weekRequest/weekRequest-details";
    }

    @PostMapping("/user/allWeekRequests/weekRequest-details/{id}/edit")
    public String editWeekRequest(@PathVariable(value = "id") long id,
                                  @RequestParam List<Integer> inputValue, @RequestParam List<Integer> inputValueWorkOff,
                                  Model model) throws ParseException, IOException {
        if (!weekRequestRepository.existsById(id)) {
            return "redirect:/user/allWeekRequests";
        }
        WeekRequest weekRequest = weekRequestRepository.findById(id).orElseThrow();
        List<ValueForWeek> allValueForWeeksWD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> x.isWorkDay())
                .collect(Collectors.toList());
        List<ValueForWeek> allValueForWeeksWOffD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> !x.isWorkDay())
                .collect(Collectors.toList());

        List<String> listTimeForWeekRequest = MyService.listTimeForWeekRequest();
        for (int i = 0; i < 24; i++) {
            ValueForWeek valueForWeek = allValueForWeeksWD.get(i);
            valueForWeek.setValue(inputValue.get(i));
            valueForWeekRepository.save(valueForWeek);
        }
        for (int i = 0; i < 24; i++) {
            ValueForWeek valueForWeek = allValueForWeeksWOffD.get(i);
            valueForWeek.setValue(inputValueWorkOff.get(i));
            valueForWeekRepository.save(valueForWeek);
        }
        return "redirect:/user/allWeekRequests/weekRequest-details/{id}";
    }

    @GetMapping("/user/allWeekRequests/weekRequest-details/{id}/remove")
    public String removeWeekRequest(@PathVariable(value = "id") long id,
                                    Model model) throws ParseException, IOException {
        if (!weekRequestRepository.existsById(id)) {
            return "redirect:/user/allWeekRequests";
        }

        WeekRequest weekRequest = weekRequestRepository.findById(id).orElseThrow();
        for (ValueForWeek valueForWeek : weekRequest.getAllValueForWeeks()) {
            valueForWeekRepository.delete(valueForWeek);
        }
        weekRequestRepository.delete(weekRequest);
        return "redirect:/user/allWeekRequests";
    }


    @GetMapping("/user/allWeekRequests/weekRequest-details/{id}/getReport")
    public ResponseEntity<Object> getReport(@PathVariable(value = "id") long id) throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        String userFullName = user.getFullName();
        WeekRequest weekRequest = weekRequestRepository.findById(id).orElseThrow();

        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");
        SimpleDateFormat simpleDateFormat_period = new SimpleDateFormat("dd.MM.yyyy");
        Date today = new Date();
        String strToday = simpleDateFormat_today.format(today);
        String strDateFrom = simpleDateFormat_today.format(weekRequest.getDateFrom());
        String strDateTo = simpleDateFormat_today.format(weekRequest.getDateTo());

        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_5.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, strDateFrom, "dateFrom");
        TemplateCreator.replacePlaceholder(wordDocument, strDateTo, "dateTo");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(weekRequest.getCountWorkDay()), "countWorkDay");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(weekRequest.getCountWorkOffDay()), "countWorkOffDay");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(weekRequest.getNumber()), "num");

        List<ValueForWeek> allValueForWeeksWD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> x.isWorkDay())
                .collect(Collectors.toList());
        List<ValueForWeek> allValueForWeeksWOffD = weekRequest.getAllValueForWeeks().stream()
                .filter(x -> !x.isWorkDay())
                .collect(Collectors.toList());

        long max = 0;
        long min = 10000;
        long summaWD = 0;
        long summaWOffD = 0;

        for (ValueForWeek valueForWeek : allValueForWeeksWD) {
            long value = valueForWeek.getValue();
            TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(value), "A");
            if (max < value) {
                max = value;
            }
            if (min > value) {
                min = value;
            }
            summaWD += value;
        }

        for (ValueForWeek valueForWeek : allValueForWeeksWOffD) {
            long value = valueForWeek.getValue();
            TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(value), "B");
            if (max < value) {
                max = value;
            }
            if (min > value) {
                min = value;
            }
            summaWOffD += value;
        }

        long middleWD = summaWD / 24;
        long middleWOffD = summaWOffD / 24;
        long allWD = summaWD * weekRequest.getCountWorkDay();
        long allWOffD = summaWOffD * weekRequest.getCountWorkOffDay();
        long allWeek = allWD + allWOffD;

        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(max), "C1");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(min), "C2");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(middleWD), "C3");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaWD), "C4");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(middleWOffD), "C5");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(summaWOffD), "C6");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(allWD), "C7");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(allWOffD), "C8");
        TemplateCreator.replacePlaceholder(wordDocument, String.valueOf(allWeek), "C9");

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }


    @PostMapping("/user/allWeekRequests/getReportAudit")
    public ResponseEntity<Object> getReportAudit() throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        String userFullName = user.getFullName();
        DecimalFormat f = new DecimalFormat("###.###");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        Set<Integer> years = new TreeSet<>();
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            Date date = new Date(dataMeter.getDateOfDataMeter().getTime());
            years.add(Integer.valueOf(simpleDateFormat.format(date)));
        }

        Set<Integer> yearsYYY = new TreeSet<>();
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            yearsYYY.add(dataMeter.getDateOfDataMeter().getYear());
        }


        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");
        SimpleDateFormat simpleDateFormat_year = new SimpleDateFormat("yyyy");
        Date today = new Date();
        String strToday = simpleDateFormat_today.format(today);
        int currentYear = Integer.parseInt(simpleDateFormat_year.format(today)) - 1;

        List<Integer> lastYears = years.stream()
                .filter(x -> x < currentYear)
                .collect(Collectors.toList());
        List<Integer> nextYears = years.stream()
                .filter(x -> x > currentYear)
                .collect(Collectors.toList());
        while (nextYears.size() < 2) {
            int newYear = nextYears.get(nextYears.size() - 1);
            newYear++;
            nextYears.add(newYear);
        }

        System.out.println("Предыдущие года - " + lastYears);
        System.out.println("Текущий год - " + currentYear);
        System.out.println("Следующие года - " + nextYears);

        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_6.docx");
        TemplateCreator.addAuditColumn(wordDocument, lastYears.size(), nextYears.size(), "lastYear");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");

        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());


        for (int lastYear : lastYears) {
            Date startYear = simpleDateFormat_year.parse(String.valueOf(lastYear));
            Date endYear = simpleDateFormat_year.parse(String.valueOf(lastYear));
            endYear.setMonth(11); endYear.setDate(31); endYear.setHours(23); endYear.setMinutes(59); endYear.setSeconds(59);
            List<Date> dateList = MyService.getDateList(startYear, endYear);
            long summaInYear = 0;
            for (Meter meter : allMeters) {
                for (Date day : dateList){
                    summaInYear += meter.getValueDataMeter_V2(day);
                }
            }
            float valueForReport =  summaInYear/1000f;
            System.out.println("Для года - " + lastYear);
            System.out.println("Start year - " + startYear);
            System.out.println("End year - " + endYear);
            System.out.println("Годовое значение - " + summaInYear);
            TemplateCreator.replacePlaceholder(wordDocument, "" + lastYear + "\n(фактическое)", "lastYear");
            TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueLY");
            TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueLY2");
        }


        for (int nextYear : nextYears) {
            Date startYear = simpleDateFormat_year.parse(String.valueOf(nextYear));
            Date endYear = simpleDateFormat_year.parse(String.valueOf(nextYear));
            endYear.setMonth(11); endYear.setDate(31); endYear.setHours(23); endYear.setMinutes(59); endYear.setSeconds(59);
            List<Date> dateList = MyService.getDateList(startYear, endYear);
            long summaInYear = 0;
            for (Meter meter : allMeters) {
                for (Date day : dateList){
                    summaInYear += meter.getPrognoses(day, yearsYYY);
                }
            }
            float valueForReport =  summaInYear/1000f;
            System.out.println("Для года - " + nextYear);
            System.out.println("Start year - " + startYear);
            System.out.println("End year - " + endYear);
            System.out.println("Годовое значение - " + summaInYear);
            TemplateCreator.replacePlaceholder(wordDocument, "" + nextYear + "\n(прогнозируемое)", "nextYear");
            TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueNY");
            TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueNY2");
        }

        Date startYear = simpleDateFormat_year.parse(String.valueOf(currentYear));
        Date endYear = simpleDateFormat_year.parse(String.valueOf(currentYear));
        endYear.setMonth(11); endYear.setDate(31); endYear.setHours(23); endYear.setMinutes(59); endYear.setSeconds(59);
        List<Date> dateList = MyService.getDateList(startYear, endYear);
        long summaInYear = 0;
        for (Meter meter : allMeters) {
            for (Date day : dateList){
                summaInYear += meter.getValueDataMeter_V2(day);
            }
        }
        float valueForReport =  summaInYear/1000f;
        TemplateCreator.replacePlaceholder(wordDocument, "" + currentYear + "\n(отчетный)", "currentYear");
        TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueCY");
        TemplateCreator.replacePlaceholder(wordDocument, f.format(valueForReport), "valueCY2");


        return TemplateCreator.downloadReport(wordDocument, "Report");
    }


}
