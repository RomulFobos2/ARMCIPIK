package com.mai.ARMCIPIK.controllers.user;

import com.mai.ARMCIPIK.models.DataPPE;
import com.mai.ARMCIPIK.models.Department;
import com.mai.ARMCIPIK.models.PPE;
import com.mai.ARMCIPIK.models.User;
import com.mai.ARMCIPIK.repo.DataPPERepository;
import com.mai.ARMCIPIK.repo.PPERepository;
import com.mai.ARMCIPIK.repo.UserRepository;
import com.mai.ARMCIPIK.service.MyService;
import com.mai.ARMCIPIK.service.TemplateCreator;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
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
import org.xlsx4j.exceptions.Xlsx4jException;
import org.xlsx4j.sml.SheetData;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

//Класс для обработки страниц администратора для работы с СИЗ
@Controller
public class UserPanelBillController {
    @Autowired
    PPERepository ppeRepository;
    @Autowired
    DataPPERepository dataPPERepository;
    @Autowired
    UserRepository userRepository;

    @GetMapping("/user/bill")
    public String bill(Model model) {
        List<PPE> allPPEs = ppeRepository.findAll();
        Set<String> allPPENames = new HashSet<>();
        for (PPE ppe : allPPEs) {
            allPPENames.add(ppe.getName());
        }
        model.addAttribute("allPPENames", allPPENames);
        return "user/bill/bill";
    }

    @PostMapping("/user/bill/getReport")
    public ResponseEntity<Object> allMeasuringToolsReport(@RequestParam String inputDepartment1, @RequestParam String inputDepartment2,
                                                          @RequestParam List<String> inputPPENames, @RequestParam List<Integer> inputCounts,
                                                          @RequestParam List<String> inputNomenclature, @RequestParam List<Integer> inputPrice,
                                                          @RequestParam List<Integer> inputSumma, @RequestParam List<String> inputLastColumn,
                                                          @RequestParam List<Integer> inputCodeEI, @RequestParam List<String> inputNameEI,

                                                          @RequestParam(required = false) String inputTypeDepartment1, @RequestParam(required = false) String inputBill,
                                                          @RequestParam(required = false) String inputTypeOperation, @RequestParam(required = false) String inputTypeDepartment2,
                                                          @RequestParam(required = false) String inputCodeAnal, @RequestParam(required = false) String inputHZ) throws IOException, Docx4JException, JAXBException, CloneNotSupportedException, ParseException, Xlsx4jException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        User user = userRepository.findByUsername(auth.getName());
        Department department = user.getDepartment();
        String userFullName = user.getFullName();
        WordprocessingMLPackage wordDocument = TemplateCreator.getTemplate("Templates reports\\Report_3.docx");
        TemplateCreator.replacePlaceholder(wordDocument, inputDepartment1, "Department1");
        TemplateCreator.replacePlaceholder(wordDocument, inputDepartment2, "Department2");

        String typeDep1 = inputTypeDepartment1 == null ? "" : inputTypeDepartment1;
        String typeDep2 = inputTypeDepartment2 == null ? "" : inputTypeDepartment2;
        String typeOperation = inputTypeOperation == null ? "" : inputTypeOperation;
        String bill = inputBill == null ? "" : inputBill;
        String anal = inputCodeAnal == null ? "" : inputCodeAnal;
        String hz = inputHZ == null ? "" : inputHZ;
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd.MM.yyyy");
        String today = simpleDateFormat.format(new Date());

        TemplateCreator.replacePlaceholder(wordDocument, typeDep1, "typeDep1");
        TemplateCreator.replacePlaceholder(wordDocument, typeDep2, "typeDep2");
        TemplateCreator.replacePlaceholder(wordDocument, typeOperation, "typeOperation");
        TemplateCreator.replacePlaceholder(wordDocument, bill, "bill");
        TemplateCreator.replacePlaceholder(wordDocument, bill, "bill");
        TemplateCreator.replacePlaceholder(wordDocument, anal, "anal");
        TemplateCreator.replacePlaceholder(wordDocument, anal, "anal");
        TemplateCreator.replacePlaceholder(wordDocument, hz, "hz");
        TemplateCreator.replacePlaceholder(wordDocument, today, "today");


        TemplateCreator.addBillToolRow(wordDocument, inputPPENames, inputCounts, inputNomenclature, inputPrice, inputSumma, inputCodeEI, inputNameEI, inputLastColumn, "name");
        return TemplateCreator.downloadReport(wordDocument, "Report");
    }
}
