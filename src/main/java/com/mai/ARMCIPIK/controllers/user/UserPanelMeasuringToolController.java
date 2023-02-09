package com.mai.ARMCIPIK.controllers.user;

import com.mai.ARMCIPIK.models.Department;
import com.mai.ARMCIPIK.models.MeasuringTool;
import com.mai.ARMCIPIK.models.User;
import com.mai.ARMCIPIK.models.Verification;
import com.mai.ARMCIPIK.repo.MeasuringToolRepository;
import com.mai.ARMCIPIK.repo.UserRepository;
import com.mai.ARMCIPIK.repo.VerificationRepository;
import com.mai.ARMCIPIK.service.OTPGenerator;
import com.mai.ARMCIPIK.service.TemplateCreator;
import com.mai.ARMCIPIK.service.UserService;
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
import java.util.*;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИ
@Controller
public class UserPanelMeasuringToolController {
    @Autowired
    MeasuringToolRepository measuringToolRepository;
    @Autowired
    VerificationRepository verificationRepository;
    @Autowired
    UserRepository userRepository;

    //Вывод страницы со всеми СИ
    @GetMapping("/user/allMeasuringTools")
    public String measuringToolsList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<MeasuringTool> allMeasuringTools = measuringToolRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        model.addAttribute("allMeasuringTools", allMeasuringTools);
        return "user/measuringTool/allMeasuringTools";
    }

    //На странице будем отображать те СИ, у которых есть текущая поверка (поверка которую необходимо запонить)
    @GetMapping("/user/allCurrentVerification")
    public String verificationList(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        List<MeasuringTool> allMeasuringTools = measuringToolRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .filter(x -> x.getVerifications().stream()
                .filter(y -> !y.isCheckVerification()).collect(Collectors.toList()).size() != 0).collect(Collectors.toList());
        model.addAttribute("allMeasuringTools", allMeasuringTools);
        return "user/measuringTool/allCurrentVerification";
    }

    //Страниц для формы для добавления СИ
    @GetMapping("/user/addMeasuringTool")
    public String addMeasuringTool(Model model) {
        List<MeasuringTool> allMeasuringTools = measuringToolRepository.findAll();
        Set<String> allMeasuringNames = new HashSet<>();
        for(MeasuringTool measuringTool : allMeasuringTools){
            allMeasuringNames.add(measuringTool.getMeasuringToolName());
        }
        model.addAttribute("allMeasuringNames", allMeasuringNames);
        return "user/measuringTool/addMeasuringTool";
    }

    //Сохраняем СИ
    @PostMapping("/user/addMeasuringTool")
    public String addMeasuringTool(@RequestParam String inputOfficeName, @RequestParam String inputDepartmentName,
                                   @RequestParam String inputMeasuringToolName, @RequestParam String inputFactoryNumber,
                                   @RequestParam String inputMeasuringRange, @RequestParam String inputAccuracyClass,
                                   @RequestParam String inputRegistrationNumberStateRegister, @RequestParam Integer inputCountVerification,
                                   @RequestParam String inputScopeOfApplication, @RequestParam String inputReducedVerification,
                                   @RequestParam String inputCheckingGRCM, @RequestParam String inputVerificationDate,
                                   @RequestParam String inputVerifyingOrganization, @RequestParam String inputCertificateNumber,
                                   @RequestParam String inputAvailability, @RequestParam String inputNextVerificationDate,
                                   @RequestParam(required = false) String inputBasisForDecommissioning,
                                   Model model) throws ParseException {
        if (measuringToolRepository.findByFactoryNumber(inputFactoryNumber) != null) {
            model.addAttribute("measuringToolFactoryNumberError", "Средство измерения с таким заводским номером уже существует");
            return "user/measuringTool/addMeasuringTool";
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();

        SimpleDateFormat simpleDateFormat_inner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Date verificationDate = simpleDateFormat_inner.parse(inputVerificationDate);
        Date nextVerificationDate = simpleDateFormat_inner.parse(inputNextVerificationDate);
        boolean availability = inputAvailability.equals("1");
        int countVerification = inputCountVerification;
        boolean scopeOfApplication = inputScopeOfApplication.equals("1");
        boolean reducedVerification = inputReducedVerification.equals("1");
        boolean checkingGRCM = inputCheckingGRCM.equals("1");

        //Первая поверка вводится с checkVerification == true.
        Verification verification = new Verification(verificationDate, inputVerifyingOrganization, inputCertificateNumber, availability, nextVerificationDate, inputBasisForDecommissioning, true);
        List<Verification> allVerifications = new ArrayList<>();
        allVerifications.add(verification);
        MeasuringTool measuringTool = new MeasuringTool(inputOfficeName, inputDepartmentName, inputMeasuringToolName, inputFactoryNumber, inputMeasuringRange,
                inputAccuracyClass, inputRegistrationNumberStateRegister, countVerification, scopeOfApplication, reducedVerification, checkingGRCM, allVerifications, department);
        measuringToolRepository.save(measuringTool);
        verification.setMeasuringTool(measuringTool);
        verificationRepository.save(verification);
        return "redirect:/user/allMeasuringTools";
    }

    //Формируем динамически страницу для СИ. Внутри страницы можно сделать операции над сущностью
    @GetMapping("/user/allMeasuringTools/measuringTool-details/{id}")
    public String measuringToolDetails(@PathVariable(value = "id") long id, Model model) {
        if (!measuringToolRepository.existsById(id)) {
            return "redirect:/user/allMeasuringTools";
        }
        MeasuringTool measuringTool = measuringToolRepository.findById(id).orElseThrow();
        model.addAttribute("measuringTool", measuringTool);

        List<Verification> allVerifications = measuringTool.getVerifications();
        model.addAttribute("allVerifications", allVerifications);
        boolean haveCurrentVerification = false;
        for (Verification verification : allVerifications) {
            //Если из всех проверок у СИ, есть хоть одна которая не проведена (checkVerification == false), то выводим кнопку, с сылкой на заполнение данных о поверки
            if (!verification.isCheckVerification()) {
                haveCurrentVerification = true;
            }
        }
        model.addAttribute("haveCurrentVerification", haveCurrentVerification);
        if (haveCurrentVerification) {
            allVerifications = measuringTool.getVerifications().stream().filter(x -> !x.isCheckVerification()).collect(Collectors.toList());
            model.addAttribute("currentVerification", allVerifications.get(0));
        }

        return "user/measuringTool/measuringTool-details";
    }

    //Удаляем СИ
    @PostMapping("/user/allMeasuringTools/measuringTool-details/{id}/remove")
    public String delete_measuringTool(@PathVariable(value = "id") long id, Model model) {

        MeasuringTool measuringTool = measuringToolRepository.findById(id).orElseThrow();
        List<Verification> allVerifications = measuringTool.getVerifications();
        for (Verification verification : allVerifications) {
            verificationRepository.delete(verification);
        }
        measuringToolRepository.delete(measuringTool);
        return "redirect:/user/allMeasuringTools";
    }


    @GetMapping("/user/editMeasuringTool/{id}")
    public String measuringToolEdit(@PathVariable(value = "id") long id,
                                    Model model, HttpServletRequest request) {
        if (!measuringToolRepository.existsById(id)) {
            return "redirect:/user/allMeasuringTools";
        }
        if (request.getSession().getAttribute("measuringToolFactoryNumberError") != null && (Boolean) request.getSession().getAttribute("measuringToolFactoryNumberError")) {
            model.addAttribute("measuringToolFactoryNumberError", "Средство измерения с таким заводским номером уже существует");
            request.getSession().setAttribute("measuringToolFactoryNumberError", false);
        }
        MeasuringTool measuringTool = measuringToolRepository.findById(id).orElseThrow();
        model.addAttribute("measuringTool", measuringTool);
        return "user/measuringTool/editMeasuringTool";
    }

    @PostMapping("/user/editMeasuringTool/{id}")
    public String measuringToolEdit(@PathVariable(value = "id") long id,
                                    @RequestParam String inputOfficeName, @RequestParam String inputDepartmentName,
                                    @RequestParam String inputMeasuringToolName, @RequestParam String inputFactoryNumber,
                                    @RequestParam String inputMeasuringRange, @RequestParam String inputAccuracyClass,
                                    @RequestParam String inputRegistrationNumberStateRegister, @RequestParam Integer inputCountVerification,
                                    @RequestParam String inputScopeOfApplication, @RequestParam String inputReducedVerification,
                                    @RequestParam String inputCheckingGRCM,
                                    Model model, HttpServletRequest request) {
        MeasuringTool measuringTool = measuringToolRepository.findById(id).orElseThrow();
        if (measuringToolRepository.findByFactoryNumber(inputFactoryNumber) != null && measuringToolRepository.findByFactoryNumber(inputFactoryNumber).getId() != id) {
            request.getSession().setAttribute("measuringToolFactoryNumberError", true);
            return "redirect:/user/editMeasuringTool/{id}";
        }
        int countVerification = inputCountVerification;
        boolean scopeOfApplication = inputScopeOfApplication.equals("1");
        boolean reducedVerification = inputReducedVerification.equals("1");
        boolean checkingGRCM = inputCheckingGRCM.equals("1");

        measuringTool.setOfficeName(inputOfficeName);
        measuringTool.setDepartmentName(inputDepartmentName);
        measuringTool.setMeasuringToolName(inputMeasuringToolName);
        measuringTool.setMeasuringRange(inputMeasuringRange);
        measuringTool.setAccuracyClass(inputAccuracyClass);
        measuringTool.setRegistrationNumberStateRegister(inputRegistrationNumberStateRegister);
        measuringTool.setCountVerification(countVerification);
        measuringTool.setScopeOfApplication(scopeOfApplication);
        measuringTool.setReducedVerification(reducedVerification);
        measuringTool.setCheckingGRCM(checkingGRCM);

        measuringToolRepository.save(measuringTool);
        return "redirect:/user/allMeasuringTools/measuringTool-details/{id}";
    }


    @GetMapping("/user/allMeasuringTools/measuringTool-details/{id}/currentVerification/{currentVerification_id}")
    public String measuringVerification(@PathVariable(value = "id") long id,
                                        @PathVariable(value = "currentVerification_id") long currentVerification_id,
                                        Model model) {
        if (!verificationRepository.existsById(currentVerification_id)) {
            return "redirect:/user/allMeasuringTools/measuringTool-details/{id}";
        }
        Verification currentVerification = verificationRepository.findById(currentVerification_id).orElseThrow();
        model.addAttribute("currentVerification", currentVerification);
        model.addAttribute("countVerification", currentVerification.getMeasuringTool().getCountVerification());
        return "user/measuringTool/currentVerification";
    }


    @PostMapping("/user/allMeasuringTools/measuringTool-details/{id}/currentVerification/{currentVerification_id}")
    public String measuringVerification(@PathVariable(value = "id") long id,
                                        @PathVariable(value = "currentVerification_id") long currentVerification_id,
                                        @RequestParam String inputVerifyingOrganization, @RequestParam String inputCertificateNumber,
                                        @RequestParam String inputAvailability, @RequestParam(required = false) String inputBasisForDecommissioning,
                                        @RequestParam String inputVerificationDate, @RequestParam String inputNextVerificationDate,
                                        Model model) throws ParseException {
        if (!verificationRepository.existsById(currentVerification_id)) {
            return "redirect:/user/allMeasuringTools/measuringTool-details/{id}";
        }
        Verification currentVerification = verificationRepository.findById(currentVerification_id).orElseThrow();
        currentVerification.setVerifyingOrganization(inputVerifyingOrganization);
        currentVerification.setCertificateNumber(inputCertificateNumber);
        boolean availability = inputAvailability.equals("1");
        currentVerification.setAvailability(availability);
        currentVerification.setBasisForDecommissioning(inputBasisForDecommissioning);
        currentVerification.setCheckVerification(true);

        SimpleDateFormat simpleDateFormat_inner = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Date verificationDate = simpleDateFormat_inner.parse(inputVerificationDate);
        Date nextVerificationDate = simpleDateFormat_inner.parse(inputNextVerificationDate);
        currentVerification.setVerificationDate(verificationDate);
        currentVerification.setNextVerificationDate(nextVerificationDate);

        verificationRepository.save(currentVerification);
        return "redirect:/user/allMeasuringTools/measuringTool-details/{id}";
    }


    @GetMapping("/user/allMeasuringTools/getReport")
    public ResponseEntity<Object> allMeasuringToolsReport() throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        String userFullName = user.getFullName();

        SimpleDateFormat simpleDateFormat_today = new SimpleDateFormat("«dd» MMMM yyyy");
        SimpleDateFormat simpleDateFormat_year = new SimpleDateFormat("yyyy");
        Date today = new Date();
        String strToday = simpleDateFormat_today.format(today);
        String strYear = simpleDateFormat_year.format(today);

        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_0.docx");
        TemplateCreator.replacePlaceholder(wordDocument, userFullName, "fio");
        TemplateCreator.replacePlaceholder(wordDocument, strYear, "year");
        TemplateCreator.replacePlaceholder(wordDocument, strToday, "today");
        TemplateCreator.replacePlaceholder(wordDocument, department.getName(), "departmentName");

        List<MeasuringTool> allMeasuringTools = measuringToolRepository.findAll().stream()
                .filter(x -> x.getDepartment().getId() == department.getId())
                .collect(Collectors.toList());
        TemplateCreator.addMeasuringToolRow(wordDocument, allMeasuringTools, "C1");

        return TemplateCreator.downloadReport(wordDocument, "Report");
    }


}
