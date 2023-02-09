package com.mai.ARMCIPIK.controllers.user;

import com.mai.ARMCIPIK.models.*;
import com.mai.ARMCIPIK.repo.DataPPERepository;
import com.mai.ARMCIPIK.repo.PPERepository;
import com.mai.ARMCIPIK.repo.UserRepository;
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
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИЗ
@Controller
public class UserPanelPPEController {
    @Autowired
    PPERepository ppeRepository;
    @Autowired
    DataPPERepository dataPPERepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/user/allPPEs")
    public String ppeList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<PPE> allPPEs = ppeRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allPPEs", allPPEs);

        for (PPE ppe : allPPEs) {
            System.out.println(ppe.getName() + " - " + ppe.getAllDataPPEs().size());
        }

        return "user/ppe/allPPEs";
    }

    @GetMapping("/user/addPPE")
    public String addPPE(Model model) {
        Date startYear = new Date();
        Date endYear = new Date();
        startYear.setMonth(0);
        startYear.setDate(1);
        startYear.setHours(0);
        startYear.setMinutes(0);
        startYear.setSeconds(0);

        endYear.setYear(endYear.getYear() + 1);
        endYear.setMonth(0);
        endYear.setDate(1);
        endYear.setHours(23);
        endYear.setMinutes(59);
        endYear.setSeconds(59);
        endYear.setDate(endYear.getDate() - 1);
        model.addAttribute("startYear", startYear);
        model.addAttribute("endYear", endYear);
        System.out.println(startYear);
        System.out.println(endYear);
        return "user/ppe/addPPE";
    }

    @PostMapping("/user/addPPE")
    public String addPPE(@RequestParam String inputName, @RequestParam int inputCount,
                         @RequestParam String inputTestDate,
                         Model model) throws ParseException, IOException {
        if (ppeRepository.findByName(inputName) != null) {
            model.addAttribute("ppeNameError", "СИЗ с таким именем уже существует");
            return "user/ppe/addPPE";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_inner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date testDate = simpleDateFormat_inner.parse(inputTestDate);

        Date today = new Date();

        if (testDate.getTime() / 1000 <= (today.getTime() / 1000)) {
            PPE ppe = new PPE(inputName, inputCount, testDate, department);
            ppeRepository.save(ppe);
            DataPPE dataPPE_old = new DataPPE(true, testDate, ppe);
            dataPPE_old.setUser(user);
            dataPPERepository.save(dataPPE_old);
            Date newTestDate = new Date(testDate.getTime());
            newTestDate.setYear(newTestDate.getYear() + 1);
            ppe.setTestDate(newTestDate);
            DataPPE dataPPE_new = new DataPPE(false, MyService.getWorkDate(newTestDate), ppe);
            dataPPERepository.save(dataPPE_new);
            ppeRepository.save(ppe);
        } else {
            PPE ppe = new PPE(inputName, inputCount, testDate, department);
            DataPPE dataPPE = new DataPPE(false, testDate, ppe);
            ppeRepository.save(ppe);
            dataPPERepository.save(dataPPE);
        }

        return "redirect:/user/allPPEs";
    }

    @GetMapping("/user/allPPEs/ppe-details/{id}")
    public String ppeDetails(@PathVariable(value = "id") long id, Model model) {
        if (!ppeRepository.existsById(id)) {
            return "redirect:/user/allPPEs";
        }
        PPE ppe = ppeRepository.findById(id).orElseThrow();
        model.addAttribute("ppe", ppe);
        return "user/ppe/ppe-details";
    }

    @PostMapping("/user/allPPEs/ppe-details/{id}/remove")
    public String delete_ppe(@PathVariable(value = "id") long id, Model model) {
        PPE ppe = ppeRepository.findById(id).orElseThrow();
        for (DataPPE dataPPE : ppe.getAllDataPPEs()) {
            dataPPERepository.delete(dataPPE);
        }
        ppeRepository.delete(ppe);
        return "redirect:/user/allPPEs";
    }


    @GetMapping("/user/editPPE/{id}")
    public String ppeEdit(@PathVariable(value = "id") long id,
                          Model model, HttpServletRequest request) {
        if (!ppeRepository.existsById(id)) {
            return "redirect:/user/allPPEs";
        }
        if (request.getSession().getAttribute("ppeNameError") != null
                && (Boolean) request.getSession().getAttribute("ppeNameError")) {
            model.addAttribute("ppeNameError",
                    "СИЗ с таким именем уже существует");
            request.getSession().setAttribute("ppeNameError", false);
        }
        PPE ppe = ppeRepository.findById(id).orElseThrow();
        DataPPE lastDataPPE = ppe.getAllDataPPEs().get(ppe.getAllDataPPEs().size() - 1);
        model.addAttribute("ppe", ppe);
        Date startYear = new Date();
        Date endYear = new Date();
        startYear.setYear(lastDataPPE.getDateOfDataPPE().getYear());
        endYear.setYear(lastDataPPE.getDateOfDataPPE().getYear());


        startYear.setMonth(0);
        startYear.setDate(1);
        startYear.setHours(0);
        startYear.setMinutes(0);
        startYear.setSeconds(0);

        endYear.setYear(endYear.getYear() + 1);
        endYear.setMonth(0);
        endYear.setDate(1);
        endYear.setHours(23);
        endYear.setMinutes(59);
        endYear.setSeconds(59);
        endYear.setDate(endYear.getDate() - 1);
        model.addAttribute("startYear", startYear);
        model.addAttribute("endYear", endYear);
        return "user/ppe/editPPE";
    }

    @PostMapping("/user/editPPE/{id}")
    public String ppeEdit(@PathVariable(value = "id") long id,
                          @RequestParam String inputName, @RequestParam int inputCount,
                          @RequestParam String inputTestDate,
                          Model model, HttpServletRequest request) throws ParseException {
        PPE ppe = ppeRepository.findById(id).orElseThrow();
        if (ppeRepository.findByName(inputName) != null && ppeRepository.findByName(inputName).getId() != id) {
            request.getSession().setAttribute("ppeNameError", true);
            return "redirect:/user/editPPE/{id}";
        }
        ppe.setName(inputName);
        ppe.setCount(inputCount);
        SimpleDateFormat simpleDateFormat_inner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Date testDate = simpleDateFormat_inner.parse(inputTestDate);
        ppe.setTestDate(testDate);
        ppeRepository.save(ppe);
        return "redirect:/user/allPPEs/ppe-details/{id}";
    }

    @GetMapping("/user/allPPEs/allCurrentDataPPEs")
    public String allCurrentDataPPEList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        Date today = new Date();
        today.setHours(0);
        today.setMinutes(0);
        today.setSeconds(0);

        List<DataPPE> allDataPPEs = dataPPERepository.findAll().stream()
                .filter(x -> x.getPpe().getDepartment().getId() == department.getId())
                .filter(x -> x.getDateOfDataPPE().getTime() / 1000 <= (today.getTime() / 1000))
                .filter(x -> !x.isFinishTest())
                .collect(Collectors.toList());
        model.addAttribute("allDataPPEs", allDataPPEs);
        return "user/ppe/allCurrentDataPPEs";
    }

    @GetMapping("/user/allPPEs/addTest/{id}")
    public String addTest(@PathVariable(value = "id") long id, Model model) throws IOException {
        if (dataPPERepository.existsById(id)) {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            User user = userRepository.findByUsername(auth.getName());
            DataPPE dataPPE = dataPPERepository.getById(id);
            Date today = new Date();
            dataPPE.setUser(user);
            dataPPE.setFinishTest(true);
            dataPPE.setDateOfDataPPE(today);
            dataPPERepository.save(dataPPE);
            Date newTestDate = new Date(today.getTime());
            newTestDate.setYear(newTestDate.getYear() + 1);
            dataPPE.getPpe().setTestDate(newTestDate);
            ppeRepository.save(dataPPE.getPpe());
            DataPPE dataPPE_new = new DataPPE(false, MyService.getWorkDate(newTestDate), dataPPE.getPpe());
            dataPPERepository.save(dataPPE_new);
        }
        return "redirect:/user/allPPEs/allCurrentDataPPEs";
    }

    @PostMapping("/user/allPPEs/statistics")
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
        List<DataPPE> allDataPPEs = dataPPERepository.findAll().stream()
                .filter(x -> x.getPpe().getDepartment().getId() == department.getId())
                .filter(x -> x.getDateOfDataPPE().getTime()/1000 >= (dateFrom.getTime()/1000))
                .filter(x -> x.getDateOfDataPPE().getTime()/1000 <= (dateTo.getTime()/1000))
                .collect(Collectors.toList());
        model.addAttribute("allDataPPEs", allDataPPEs);
        model.addAttribute("department", department);
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);
        return "user/ppe/statistics";
    }

    @PostMapping("/user/allPPEs/getReport_1")
    public ResponseEntity<Object> ppeReport_1(@RequestParam String resultFromInput, @RequestParam String resultToInput) throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
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


        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_2.1.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, strDateFrom, "dateFrom");
        TemplateCreator.replacePlaceholder(wordDocument, strDateTo, "dateTo");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");


        List<DataPPE> allDataPPEs = dataPPERepository.findAll().stream()
                .filter(x -> x.getPpe().getDepartment().getId() == department.getId())
                .filter(x -> x.getDateOfDataPPE().getTime()/1000 >= (dateFrom.getTime()/1000))
                .filter(x -> x.getDateOfDataPPE().getTime()/1000 <= (dateTo.getTime()/1000))
                .collect(Collectors.toList());

        TemplateCreator.addPPERow_1(wordDocument, allDataPPEs, "num");

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }


    @GetMapping("/user/allPPEs/getReport_2")
    public ResponseEntity<Object> ppeReport_2() throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        String userFullName = user.getFullName();
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");

        Date today = new Date();
        //таким образом, в дате dateTo всегда получаем последний день месяца
        //И т.о. у нас период будет с 1 числа dateFrom по 31/30/28/29 число dateTo
        String strToday = simpleDateFormat_today.format(today);


        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_2.2.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");


        List<PPE> allPPEs = ppeRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());

        TemplateCreator.addPPERow_2(wordDocument, allPPEs, "num");

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }

}
