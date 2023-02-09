package com.mai.ARMCIPIK.controllers.user;

import com.mai.ARMCIPIK.models.*;
import com.mai.ARMCIPIK.repo.*;
import com.mai.ARMCIPIK.service.MyService;
import com.mai.ARMCIPIK.service.TemplateCreator;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
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
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИ
@Controller
public class UserPanelMeterController {
    @Autowired
    MeterRepository meterRepository;
    @Autowired
    DataMeterRepository dataMeterRepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/user/allMeters")
    public String metersList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allMeters", allMeters);

//        System.out.println("Счетчик - " + allMeters.get(0).getName());
//        Date date = new Date();
//        date.setYear(121);
//        Set<Integer> yearsDataMeter = new TreeSet<>();
//        for (DataMeter dataMeter : allMeters.get(0).getAllDataMeters()) {
//            yearsDataMeter.add(dataMeter.getDateOfDataMeter().getYear());
//        }
//        allMeters.get(0).getPrognoses(date, yearsDataMeter);


        return "user/meter/allMeters";
    }

    @GetMapping("/user/addMeter")
    public String addMeasuringTool(Model model) {
        return "user/meter/addMeter";
    }

    @PostMapping("/user/addMeter")
    public String addMeter(@RequestParam String inputName, Model model) {
        if (meterRepository.findByName(inputName) != null) {
            model.addAttribute("meterNameError", "Счетчик с таким именем уже существует");
            return "user/meter/addMeter";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        Meter meter = new Meter(inputName, department);
        meterRepository.save(meter);
        return "redirect:/user/allMeters";
    }

    @GetMapping("/user/allMeters/meter-details/{id}")
    public String meterDetails(@PathVariable(value = "id") long id, Model model) {
        if (!meterRepository.existsById(id)) {
            return "redirect:/user/allMeters";
        }
        Meter meter = meterRepository.findById(id).orElseThrow();
        model.addAttribute("meter", meter);
        Set<String> yearsDataMeter = new TreeSet<>();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy");
        for (DataMeter dataMeter : meter.getAllDataMeters()) {
            String year = simpleDateFormat.format(dataMeter.getDateOfDataMeter());
            yearsDataMeter.add(year);
        }
        System.out.println(yearsDataMeter);
        model.addAttribute("yearsDataMeter", yearsDataMeter);

        boolean haveCurrentDataMeter = false;
        for (DataMeter dataMeter : meter.getAllDataMeters()) {
            //Если из всех проверок у СИ, есть хоть одна которая не проведена (checkVerification == false), то выводим кнопку, с сылкой на заполнение данных о поверки
            if (dataMeter.getValueMeter() == 0) {
                haveCurrentDataMeter = true;
            }
        }
        model.addAttribute("haveCurrentDataMeter", haveCurrentDataMeter);
        if (haveCurrentDataMeter) {
            DataMeter currentDataMeter = meter.getAllDataMeters().stream().filter(x -> x.getValueMeter() == 0).collect(Collectors.toList()).get(0);
            model.addAttribute("currentDataMeter", currentDataMeter);
        }

        return "user/meter/meter-details";
    }

    @PostMapping("/user/allMeters/meter-details/{id}/remove")
    public String delete_meter(@PathVariable(value = "id") long id, Model model) {
        Meter meter = meterRepository.findById(id).orElseThrow();
        List<DataMeter> allDataMeters = meter.getAllDataMeters();
        for (DataMeter dataMeter : allDataMeters) {
            dataMeterRepository.delete(dataMeter);
        }
        //meterRepository.delete(meter);
        return "redirect:/user/allMeters";
    }


    @GetMapping("/user/editMeter/{id}")
    public String meterEdit(@PathVariable(value = "id") long id,
                            Model model, HttpServletRequest request) {
        if (!meterRepository.existsById(id)) {
            return "redirect:/user/allMeters";
        }
        if (request.getSession().getAttribute("meterNameError") != null && (Boolean) request.getSession().getAttribute("meterNameError")) {
            model.addAttribute("meterNameError", "Счетчик с таким именем уже существует");
            request.getSession().setAttribute("meterNameError", false);
        }
        Meter meter = meterRepository.findById(id).orElseThrow();
        model.addAttribute("meter", meter);
        return "user/meter/editMeter";
    }

    @PostMapping("/user/editMeter/{id}")
    public String meterEdit(@PathVariable(value = "id") long id,
                            @RequestParam String inputName,
                            Model model, HttpServletRequest request) {
        Meter meter = meterRepository.findById(id).orElseThrow();
        if (meterRepository.findByName(inputName) != null && meterRepository.findByName(inputName).getId() != id) {
            request.getSession().setAttribute("meterNameError", true);
            return "redirect:/user/editMeter/{id}";
        }
        meter.setName(inputName);
        meterRepository.save(meter);
        return "redirect:/user/allMeters/meter-details/{id}";
    }


    @GetMapping("/user/allMeters/meter-details/{meter_id}/currentDataMeter/{currentDataMeter_id}")
    public String currentDataMeter(@PathVariable(value = "meter_id") long meter_id,
                                   @PathVariable(value = "currentDataMeter_id") long currentDataMeter_id,
                                   Model model) {
        if (!meterRepository.existsById(meter_id) | !dataMeterRepository.existsById(currentDataMeter_id)) {
            return "redirect:/user/allMeters/meter-details/{id}";
        }
        Meter meter = meterRepository.findById(meter_id).orElseThrow();
        model.addAttribute("meter", meter);
        return "user/meter/currentDataMeter";
    }

    @PostMapping("/user/allMeters/meter-details/{meter_id}/currentDataMeter/{currentDataMeter_id}")
    public String currentDataMeter(@PathVariable(value = "meter_id") long meter_id,
                                   @PathVariable(value = "currentDataMeter_id") long currentDataMeter_id,
                                   @RequestParam long inputValueMeter,
                                   Model model) {
        if (!meterRepository.existsById(meter_id) | !dataMeterRepository.existsById(currentDataMeter_id)) {
            return "redirect:/user/allMeters/meter-details/{id}";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        DataMeter dataMeter = dataMeterRepository.findById(currentDataMeter_id).orElseThrow();
        dataMeter.setValueMeter(inputValueMeter);
        dataMeter.setUser(user);
        dataMeterRepository.save(dataMeter);

        //После снятия показаний, пересчитываем у всех показаний разницу
        meterRepository.findById(meter_id).orElseThrow().reCalculateDifferentValue();

        return "redirect:/user/allMeters/meter-details/{meter_id}";
    }

    @GetMapping("/user/allCurrentDataMeters")
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

        List<DataMeter> allDataMeters = dataMeterRepository.findAll().stream()
                .filter(x -> x.getMeter().getDepartment().getId() == department.getId())
                .filter(x -> x.getValueMeter() == 0)
                .collect(Collectors.toList());
        model.addAttribute("allDataMeters", allDataMeters);
        return "user/meter/allCurrentDataMeters";
    }

    @PostMapping("/user/allCurrentDataMeters")
    public String allCurrentDataMetersList(@RequestParam List<Long> dataMeterId,
                                           @RequestParam List<Long> inputValueMeter, Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        for (int i = 0; i < dataMeterId.size(); i++) {
            DataMeter dataMeter = dataMeterRepository.findById(dataMeterId.get(i)).orElseThrow();
            dataMeter.setValueMeter(inputValueMeter.get(i));
            dataMeter.setUser(user);
            dataMeterRepository.save(dataMeter);
            dataMeter.getMeter().reCalculateDifferentValue();
            meterRepository.save(dataMeter.getMeter());
        }
        //Берем только те показания, которые созданы для сегодняшнего числа
        return "redirect:/user/allMeters";
    }

    @PostMapping("/user/allMeters/statistics")
    public String statistics(@RequestParam String resultFromInput, @RequestParam String resultToInput,
                             Model model) throws ParseException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_PeriodInner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat simpleDateFormat_PeriodOut = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Date dateFrom = simpleDateFormat_PeriodInner.parse(resultFromInput);
        Date dateTo = simpleDateFormat_PeriodInner.parse(resultToInput);
        dateTo.setHours(23);
        dateTo.setMinutes(59);
        String strDateFrom = simpleDateFormat_PeriodOut.format(dateFrom);
        String strDateTo = simpleDateFormat_PeriodOut.format(dateTo);
//        System.out.println("strDateFrom - " + strDateFrom);
//        System.out.println("strDateTo - " + strDateTo);
        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allMeters", allMeters);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        List<Date> dateList = MyService.getDateList(dateFrom, dateTo);
        model.addAttribute("dateList", dateList);

        for (Meter meter : allMeters) {
            System.out.println("Размер МАПЫ ДО - " + meter.getAllValueDataMeters().size());
        }

        calculateValueDataMeters(allMeters, dateList, dateFrom, dateTo);

        Set<Integer> yearsDataMeter = new TreeSet<>();
        //for (DataMeter dataMeter : allMeters.get(0).getAllDataMeters()) {
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            yearsDataMeter.add(dataMeter.getDateOfDataMeter().getYear());
        }
        model.addAttribute("yearsDataMeter", yearsDataMeter);


        long summaFact = 0;
        long summaPrognoses = 0;
        for (Meter meter : allMeters) {
            System.out.println("Размер МАПЫ ПОСЛЕ - " + meter.getAllValueDataMeters().size());
            for (Map.Entry<Date, Long> entry : meter.getAllValueDataMeters().entrySet()) {
                summaFact += entry.getValue();
            }
            for (Date date : dateList) {
                long var = meter.getPrognoses(date, yearsDataMeter);
                summaPrognoses += var;
            }
        }

        model.addAttribute("summaFact", summaFact);
        model.addAttribute("summaPrognoses", summaPrognoses);
        model.addAttribute("different", summaFact - summaPrognoses);


        List<Long> factForChart = new ArrayList();
        List<Long> prognozForChart = new ArrayList();
        List<String> dateForChart = new ArrayList();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");

        for (Date date : dateList) {
            dateForChart.add(simpleDateFormat.format(date));
            long summaForPrognoz = 0;
            long summaForFact = 0;
            for (Meter meter : allMeters) {
                summaForPrognoz += meter.getPrognoses(date, yearsDataMeter);
                summaForFact += meter.getValueDataMeter_V2(date);
            }
            prognozForChart.add(summaForPrognoz);//Для каждоый даты есть прогноз по всем счетчикам
            factForChart.add(summaForFact);//Для каждоый даты есть прогноз по всем счетчикам
        }

        System.out.println("factForChart: " + factForChart);
        System.out.println("prognozForChart : " + prognozForChart);

        model.addAttribute("factForChart", factForChart);
        model.addAttribute("prognozForChart", prognozForChart);
        model.addAttribute("dateForChart", dateForChart);

        return "user/meter/statistics";
    }

    @GetMapping("/user/allMeters/import")
    public String addImport(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allMeters", allMeters);
        return "user/meter/import";
    }


    @PostMapping("/user/allMeters/import")
    public String addImport(@RequestParam Long inputMeter,
                            @RequestParam("inputFileField") MultipartFile file,
                            Model model) throws IOException {
        if (!meterRepository.existsById(inputMeter)) {
            return "redirect:/user/allMeters";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Meter meter = meterRepository.findById(inputMeter).orElseThrow();

        HSSFWorkbook wb = new HSSFWorkbook(file.getInputStream());
        System.out.println("Кол-во листов = " + wb.getNumberOfSheets());
        System.out.println("Размер дата метр у счетчика ДО - " + meter.getName() + " = " + meter.getAllDataMeters().size());
        for (int i = 0; i < wb.getNumberOfSheets(); i++) {
            Set<DataMeter> dataMeterHashSet = MyService.parse(wb.getSheetAt(i), meter.getName(), wb, meter, user);
            for (DataMeter dataMeter : dataMeterHashSet) {
                meter.getAllDataMeters().add(dataMeter);
                meterRepository.save(meter);
                dataMeterRepository.save(dataMeter);
            }

        }
        System.out.println("Размер дата метр у счетчика ПОСЛЕ - " + meter.getName() + " = " + meter.getAllDataMeters().size());
        System.out.println("--------------------------------------------------------------------");
        return "redirect:/user/allMeters/meter-details/" + inputMeter;
    }


    @PostMapping("/user/allMeters/getReport")
    public ResponseEntity<Object> allMeasuringToolsReport(@RequestParam String resultFromInput,
                                                          @RequestParam String resultToInput) throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        String userFullName = user.getFullName();
        Department department = user.getDepartment();

        System.out.println("Пришли даты");
        System.out.println(resultFromInput);
        System.out.println(resultToInput);

        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");
        SimpleDateFormat simpleDateFormat_PeriodInner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat simpleDateFormat_PeriodOut = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());

        Date today = new Date();
        Date dateFrom = simpleDateFormat_PeriodInner.parse(resultFromInput);
        Date dateTo = simpleDateFormat_PeriodInner.parse(resultToInput);

        dateTo.setHours(23);
        dateTo.setMinutes(59);
        String strToday = simpleDateFormat_today.format(today);
        String strDateFrom = simpleDateFormat_PeriodOut.format(dateFrom);
        String strDateTo = simpleDateFormat_PeriodOut.format(dateTo);
        System.out.println("strDateFrom - " + strDateFrom);
        System.out.println("strDateTo - " + strDateTo);

        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_4.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, strDateFrom, "dateFrom");
        TemplateCreator.replacePlaceholder(wordDocument, strDateTo, "dateTo");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");

        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        TemplateCreator.addMeterColumn(wordDocument, allMeters, "meterName");
        for (Meter meter : allMeters) {
            TemplateCreator.replacePlaceholder(wordDocument, meter.getName(), "meterName");
        }

        Set<Integer> yearsDataMeter = new TreeSet<>();
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            yearsDataMeter.add(dataMeter.getDateOfDataMeter().getYear());
        }

        List<Date> dateList = MyService.getDateList(dateFrom, dateTo);
        calculateValueDataMeters(allMeters, dateList, dateFrom, dateTo);
        TemplateCreator.addMeterToolRow(wordDocument, allMeters, dateList, yearsDataMeter, "date");

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }


    //Считаем потребление
    public void calculateValueDataMeters(List<Meter> allMeters, List<Date> dateList,
                                         Date dateFrom, Date dateTo) {
        for (Meter meter : allMeters) {
            Date minDate = new Date();
            Date maxDate = new Date();
            for (DataMeter dataMeter : meter.getAllDataMeters()) {
                if (dataMeter.getDateOfDataMeter().getTime() < minDate.getTime()) {
                    minDate.setTime(dataMeter.getDateOfDataMeter().getTime());
                }
                if (dataMeter.getDateOfDataMeter().getTime() > maxDate.getTime()) {
                    maxDate.setTime(dataMeter.getDateOfDataMeter().getTime());
                }
            }
            maxDate.setDate(maxDate.getDate() - 1);
            if (dateFrom.getTime() > minDate.getTime()) {
                minDate.setTime(dateFrom.getTime());
            }
            if (dateTo.getTime() < maxDate.getTime()) {
                maxDate.setTime(dateTo.getTime());
            }
            System.out.println("Будем считать для дат");
            System.out.println(minDate);
            System.out.println(maxDate);
            Date varDate = new Date(minDate.getTime());
            //Кладем в мапу все даты за период
            while (varDate.getTime() <= maxDate.getTime()) {
                meter.getAllValueDataMeters().put(new Date(varDate.getTime()), 0l);
                varDate.setDate(varDate.getDate() + 1);
            }
            //Здесь показания счетчиков за период который интересует
            List<DataMeter> dataMeterList = meter.getAllDataMeters().stream()
                    .filter(x -> x.getDateOfDataMeter().getTime() / 1000 >= (minDate.getTime() / 1000))
                    .filter(x -> x.getDateOfDataMeter().getTime() / 1000 < (maxDate.getTime() / 1000))
                    .collect(Collectors.toList());
            for (Map.Entry<Date, Long> entry : meter.getAllValueDataMeters().entrySet()) {
                Date currentDate = new Date(entry.getKey().getTime());
                System.out.println("Считаем потребление для даты - " + currentDate);
                entry.setValue(meter.getValueDataMeter_V2(currentDate));
            }
        }
    }


    @PostMapping("/user/allMeters/prognoz")
    public String prognoz(@RequestParam String resultFromInput, @RequestParam String resultToInput,
                             Model model) throws ParseException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_PeriodInner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat simpleDateFormat_PeriodOut = new SimpleDateFormat("dd MMMM yyyy", Locale.getDefault());
        Date dateFrom = simpleDateFormat_PeriodInner.parse(resultFromInput);
        Date dateTo = simpleDateFormat_PeriodInner.parse(resultToInput);
        dateTo.setHours(23);
        dateTo.setMinutes(59);
        String strDateFrom = simpleDateFormat_PeriodOut.format(dateFrom);
        String strDateTo = simpleDateFormat_PeriodOut.format(dateTo);
//        System.out.println("strDateFrom - " + strDateFrom);
//        System.out.println("strDateTo - " + strDateTo);
        List<Meter> allMeters = meterRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allMeters", allMeters);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        List<Date> dateList = MyService.getDateList(dateFrom, dateTo);
        model.addAttribute("dateList", dateList);

        for (Meter meter : allMeters) {
            System.out.println("Размер МАПЫ ДО - " + meter.getAllValueDataMeters().size());
        }

        calculateValueDataMeters(allMeters, dateList, dateFrom, dateTo);

        Set<Integer> yearsDataMeter = new TreeSet<>();
        //for (DataMeter dataMeter : allMeters.get(0).getAllDataMeters()) {
        for (DataMeter dataMeter : dataMeterRepository.findAll()) {
            yearsDataMeter.add(dataMeter.getDateOfDataMeter().getYear());
        }
        model.addAttribute("yearsDataMeter", yearsDataMeter);


        long summaFact = 0;
        long summaPrognoses = 0;
        for (Meter meter : allMeters) {
            System.out.println("Размер МАПЫ ПОСЛЕ - " + meter.getAllValueDataMeters().size());
            for (Map.Entry<Date, Long> entry : meter.getAllValueDataMeters().entrySet()) {
                summaFact += entry.getValue();
            }
            for (Date date : dateList) {
                long var = meter.getPrognoses(date, yearsDataMeter);
                summaPrognoses += var;
            }
        }

        model.addAttribute("summaFact", summaFact);
        model.addAttribute("summaPrognoses", summaPrognoses);
        model.addAttribute("different", summaFact - summaPrognoses);


        List<Long> factForChart = new ArrayList();
        List<Long> prognozForChart = new ArrayList();
        List<String> dateForChart = new ArrayList();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yy");

        for (Date date : dateList) {
            dateForChart.add(simpleDateFormat.format(date));
            long summaForPrognoz = 0;
            long summaForFact = 0;
            for (Meter meter : allMeters) {
                summaForPrognoz += meter.getPrognoses(date, yearsDataMeter);
                summaForFact += meter.getValueDataMeter_V2(date);
            }
            prognozForChart.add(summaForPrognoz);//Для каждоый даты есть прогноз по всем счетчикам
            factForChart.add(summaForFact);//Для каждоый даты есть прогноз по всем счетчикам
        }

        System.out.println("factForChart: " + factForChart);
        System.out.println("prognozForChart : " + prognozForChart);

        model.addAttribute("factForChart", factForChart);
        model.addAttribute("prognozForChart", prognozForChart);
        model.addAttribute("dateForChart", dateForChart);

        return "user/meter/prognoz";
    }

}
