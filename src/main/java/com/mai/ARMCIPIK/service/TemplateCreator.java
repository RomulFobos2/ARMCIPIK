package com.mai.ARMCIPIK.service;

import com.mai.ARMCIPIK.models.*;
import org.docx4j.XmlUtils;
import org.docx4j.dml.CTBlip;
import org.docx4j.openpackaging.exceptions.Docx4JException;
import org.docx4j.openpackaging.exceptions.InvalidFormatException;
import org.docx4j.openpackaging.packages.SpreadsheetMLPackage;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.docx4j.openpackaging.parts.PartName;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorkbookPart;
import org.docx4j.openpackaging.parts.SpreadsheetML.WorksheetPart;
import org.docx4j.openpackaging.parts.relationships.RelationshipsPart;
import org.docx4j.relationships.Relationship;
import org.docx4j.wml.*;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.xlsx4j.exceptions.Xlsx4jException;
import org.xlsx4j.org.apache.poi.ss.usermodel.DataFormatter;
import org.xlsx4j.sml.Cell;
import org.xlsx4j.sml.Row;
import org.xlsx4j.sml.SheetData;
import org.xlsx4j.sml.Worksheet;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.io.*;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;


@Service
public class TemplateCreator {
    static Long summaFactValue = 0l;
    static Long summaPrognozValue = 0l;

    public static WordprocessingMLPackage getTemplate(String name) throws Docx4JException, FileNotFoundException {
        WordprocessingMLPackage template = WordprocessingMLPackage.load(new FileInputStream(new File(name)));
        return template;
    }

    public static List<Object> getAllElementFromObject(Object obj, Class<?> toSearch) {
        List<Object> result = new ArrayList<Object>();
        if (obj instanceof JAXBElement) obj = ((JAXBElement<?>) obj).getValue();

        if (obj.getClass().equals(toSearch))
            result.add(obj);
        else if (obj instanceof ContentAccessor) {
            List<?> children = ((ContentAccessor) obj).getContent();
            for (Object child : children) {
                result.addAll(getAllElementFromObject(child, toSearch));
            }
        }
        return result;
    }


    public static void replacePlaceholder(WordprocessingMLPackage template, String name, String placeholder) {
        List<Object> texts = getAllElementFromObject(template.getMainDocumentPart(), Text.class);
        for (Object text : texts) {
            Text textElement = (Text) text;
            if (textElement.getValue().equals(placeholder)) {
                textElement.setValue(name);
                break; //Не забываем
            }
        }
    }


    public static void replacePlaceholderInTR(Tr tr, String name, String placeholder) {
        List<Object> texts = getAllElementFromObject(tr, Text.class);
        for (Object text : texts) {
            Text textElement = (Text) text;
            if (textElement.getValue().equals(placeholder)) {
                textElement.setValue(name);
                break;
            }
        }
    }

    public static Tbl getTemplateTable(List<Object> tables, String templateKey) throws Docx4JException, JAXBException {
        for (Iterator<Object> iterator = tables.iterator(); iterator.hasNext(); ) {
            Object tbl = iterator.next();
            List<?> textElements = getAllElementFromObject(tbl, Text.class);
            for (Object text : textElements) {
                Text textElement = (Text) text;
                if (textElement.getValue() != null && textElement.getValue().equals(templateKey))
                    return (Tbl) tbl;
            }
        }
        return null;
    }


    public static ResponseEntity<Object> downloadReport(WordprocessingMLPackage template, String target) throws IOException, Docx4JException {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("ddMMyyyy-HHmmss", Locale.getDefault());
        String strDate = simpleDateFormat_out.format(date);
        String fileName = target + "-" + strDate + ".docx";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        template.save(outputStream);
        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        InputStreamResource resource = new InputStreamResource(inputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        ResponseEntity<Object>
                responseEntity = ResponseEntity.ok().headers(headers).contentLength(outputStream.size()).contentType(
                MediaType.parseMediaType("application/txt")).body(resource);
        return responseEntity;
    }

    public static SpreadsheetMLPackage getTemplateExcel(String name) throws Docx4JException, FileNotFoundException {
        SpreadsheetMLPackage template = SpreadsheetMLPackage.load(new File(name));
        return template;
    }

    public static void replacePlaceholderExcel(SpreadsheetMLPackage template, String name, String placeholder) throws Docx4JException, Xlsx4jException {
        WorkbookPart workbookPart = template.getWorkbookPart();
        WorksheetPart worksheetPart = workbookPart.getWorksheet(0);
        SheetData sheetData = worksheetPart.getContents().getSheetData();
        List<Row> rows = worksheetPart.getContents().getSheetData().getRow();
        boolean check = true;
        for (int i = 0; i < rows.size(); i++) {
            List<Cell> cell = rows.get(i).getC();
            DataFormatter formatter = new DataFormatter();
            String s = formatter.formatCellValue(cell.get(0));
            System.out.println("s - " + s);
            for (int j = 0; j < cell.size(); j++) {
                Cell c = cell.get(j);
                System.out.println(c.getR() + " содержит --> " + c.getV());
                s = formatter.formatCellValue(c);
                System.out.println("s - " + s);
                if (s.equals("name") & check) {
                    Row newRow = (Row) XmlUtils.deepCopy(rows.get(i), rows.get(i).getWorksheetPart().getJAXBContext());
                    rows.add(newRow);
                    check = false;
                }
            }
        }
    }


    public static ResponseEntity<Object> downloadReportExcel(SpreadsheetMLPackage template, String target) throws IOException, Docx4JException {
        Date date = new Date();
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("ddMMyyyy-HHmmss", Locale.getDefault());
        String strDate = simpleDateFormat_out.format(date);
        String fileName = target + "-" + strDate + ".xlsx";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        template.save(outputStream);
        byte[] bytes = outputStream.toByteArray();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        InputStreamResource resource = new InputStreamResource(inputStream);
        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", String.format("attachment; filename=\"%s\"", fileName));
        headers.add("Cache-Control", "no-cache, no-store, must-revalidate");
        headers.add("Pragma", "no-cache");
        headers.add("Expires", "0");
        ResponseEntity<Object>
                responseEntity = ResponseEntity.ok().headers(headers).contentLength(outputStream.size()).contentType(
                MediaType.parseMediaType("application/txt")).body(resource);
        return responseEntity;
    }

    //ПЛАН-ГРАФИК поверки средств измерений
    public static void addMeasuringToolRow(WordprocessingMLPackage template, List<MeasuringTool> allMeasuringTools, String placeholder) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(1);
        for (MeasuringTool measuringTool : allMeasuringTools) {
            addRowToTableMeasuringTool(tempTable, templateRow, measuringTool);
        }
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }


    public static void addRowToTableMeasuringTool(Tbl reviewTable, Tr templateRow, MeasuringTool measuringTool) {
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("dd.MM.yyyy");
        Verification lastVerification = measuringTool.getVerifications().get(measuringTool.getVerifications().size() - 1);
        for (Object object : textElements) {
            Text text = (Text) object;
            if (text.getValue().equals("C1")) {
                text.setValue(String.valueOf(measuringTool.getOfficeName()));
            }
            if (text.getValue().equals("C2")) {
                text.setValue(measuringTool.getDepartmentName());
            }
            if (text.getValue().equals("C3")) {
                text.setValue(measuringTool.getMeasuringToolName());
            }
            if (text.getValue().equals("C4")) {
                text.setValue(measuringTool.getFactoryNumber());
            }
            if (text.getValue().equals("C5")) {
                text.setValue(measuringTool.getMeasuringRange());
            }
            if (text.getValue().equals("C6")) {
                text.setValue(measuringTool.getAccuracyClass());
            }
            if (text.getValue().equals("C7")) {
                text.setValue(measuringTool.getRegistrationNumberStateRegister());
            }
            if (text.getValue().equals("C8")) {
                text.setValue(String.valueOf(measuringTool.getCountVerification()));
            }
            if (text.getValue().equals("C9")) {
                if (measuringTool.isScopeOfApplication()) {
                    text.setValue("СГОЕИ");
                } else {
                    text.setValue("НЕТ");
                }
            }
            if (text.getValue().equals("C10")) {
                if (measuringTool.isReducedVerification()) {
                    text.setValue("СП");
                } else {
                    text.setValue("НЕТ");
                }
            }
            if (text.getValue().equals("C11")) {
                if (measuringTool.isCheckingGRCM()) {
                    text.setValue("ГРМЦ");
                } else {
                    text.setValue("НЕТ");
                }
            }
            if (text.getValue().equals("C12")) {
                String strDate = simpleDateFormat_out.format(lastVerification.getVerificationDate());
                text.setValue(strDate);
            }
            if (text.getValue().equals("C13")) {
                text.setValue(lastVerification.getVerifyingOrganization());
            }
            if (text.getValue().equals("C14")) {
                text.setValue(lastVerification.getCertificateNumber());
            }
            if (text.getValue().equals("C15")) {
                if (lastVerification.isAvailability()) {
                    text.setValue("Пригоден");
                } else {
                    text.setValue("Не пригоден");
                }
            }
            if (text.getValue().equals("C16")) {
                String strDate = simpleDateFormat_out.format(lastVerification.getNextVerificationDate());
                text.setValue(strDate);

            }
            if (text.getValue().equals("C17")) {
                text.setValue(lastVerification.getBasisForDecommissioning());
            }
        }
        reviewTable.getContent().add(workingRow);
    }


    //Отчет по водопотреблению
    public static void addWaterMeterRow(WordprocessingMLPackage template, List<WaterMeter> allWaterMeters, String placeholder, List<Date> monthList) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(2);
        int num = 1;
        for (Date month : monthList) {
            for (WaterMeter waterMeter : allWaterMeters) {
                if (waterMeter.checkDataWaterMeter(month)) {
                    addRowToTableWaterMeter(tempTable, templateRow, waterMeter, month, num);
                    num++;
                }
            }
        }
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }

    public static void addRowToTableWaterMeter(Tbl reviewTable, Tr templateRow, WaterMeter waterMeter, Date month, int num) {
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("yyyy");
        String strMonth = MyService.getMonthNameStatic(month);
        String strYear = simpleDateFormat_out.format(month);
        long valueInStartMonth = waterMeter.findDataWaterMeter(month, true) == null ? 0 : waterMeter.findDataWaterMeter(month, true).getValueMeter();
        long valueInEndMonth = waterMeter.findDataWaterMeter(month, false) == null ? 0 : waterMeter.findDataWaterMeter(month, false).getValueMeter();
        String valueInMonth = waterMeter.calcValueInMonth(month);
        for (Object object : textElements) {
            Text text = (Text) object;
            if (text.getValue().equals("num")) {
                text.setValue(String.valueOf(num));
            }
            if (text.getValue().equals("valueMonth")) {
                text.setValue(strMonth);
            }
            if (text.getValue().equals("valueYear")) {
                text.setValue(strYear);
            }
            if (text.getValue().equals("cabinet")) {
                text.setValue(waterMeter.getCabinet());
            }
            if (text.getValue().equals("name")) {
                text.setValue(waterMeter.getName());
            }
            if (text.getValue().equals("factoryNumber")) {
                text.setValue(waterMeter.getFactoryNumber());
            }
            if (text.getValue().equals("startM")) {
                text.setValue(String.valueOf(valueInStartMonth));
            }
            if (text.getValue().equals("endM")) {
                text.setValue(String.valueOf(valueInEndMonth));
            }
            if (text.getValue().equals("summa")) {
                text.setValue(valueInMonth);
            }
            if (text.getValue().equals("type")) {
                if (waterMeter.isDrinkWater()) text.setValue("Счетчик питьевой воды");
                if (waterMeter.isHotWater()) text.setValue("Счетчик горячей воды");
                if (waterMeter.isTechWater()) text.setValue("Счетчик технической воды");
            }
        }
        reviewTable.getContent().add(workingRow);
    }

    //График испытаний СИЗ
    public static void addPPERow_1(WordprocessingMLPackage template, List<DataPPE> allDataPPEs, String placeholder) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(2);
        int num = 1;
        for (DataPPE dataPPE : allDataPPEs) {
            addRowToTablePPE_1(tempTable, templateRow, dataPPE, num);
            num++;
        }
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }

    public static void addRowToTablePPE_1(Tbl reviewTable, Tr templateRow, DataPPE dataPPE, int num) {
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("dd.MM.yyyy");
        for (Object object : textElements) {
            Text text = (Text) object;
            if (text.getValue().equals("num")) {
                text.setValue(String.valueOf(num));
            }
            if (text.getValue().equals("name")) {
                text.setValue(dataPPE.getPpe().getName());
            }
            if (text.getValue().equals("count")) {
                text.setValue(String.valueOf(dataPPE.getPpe().getCount()));
            }
            if (text.getValue().equals("date")) {
                text.setValue(simpleDateFormat_out.format(dataPPE.getDateOfDataPPE()));
            }
            if (text.getValue().equals("test")) {
                if (dataPPE.isFinishTest()) {
                    text.setValue("Проведено");
                } else {
                    text.setValue("Не проведено");
                }

            }
        }
        reviewTable.getContent().add(workingRow);
    }


    //График испытаний СИЗ
    public static void addPPERow_2(WordprocessingMLPackage template, List<PPE> allPPEs, String placeholder) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(2);
        int num = 1;
        for (PPE ppe : allPPEs) {
            addRowToTablePPE_2(tempTable, templateRow, ppe, num);
            num++;
        }
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }

    public static void addRowToTablePPE_2(Tbl reviewTable, Tr templateRow, PPE ppe, int num) {
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("dd.MM.yyyy");
        for (Object object : textElements) {
            Text text = (Text) object;
            if (text.getValue().equals("num")) {
                text.setValue(String.valueOf(num));
            }
            if (text.getValue().equals("name")) {
                text.setValue(ppe.getName());
            }
            if (text.getValue().equals("count")) {
                text.setValue(String.valueOf(ppe.getCount()));
            }
            if (text.getValue().equals("date")) {
                text.setValue(simpleDateFormat_out.format(ppe.getTestDate()));
            }
        }
        reviewTable.getContent().add(workingRow);
    }

    //Накладная
    public static void addBillToolRow(WordprocessingMLPackage template, List<String> inputPPENames, List<Integer> inputCounts,
                                      List<String> inputNomenclature, List<Integer> inputPrice, List<Integer> inputSumma,
                                      List<Integer> inputCodeEI, List<String> inputNameEI, List<String> inputLastColumn,
                                      String placeholder) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(14);
        int num = 1;
        for (int i = 0; i < inputPPENames.size(); i++) {
            addRowToTableBill(tempTable, templateRow, inputPPENames.get(i), inputCounts.get(i),
                    inputNomenclature.get(i), inputPrice.get(i), inputSumma.get(i),
                    inputCodeEI.get(i), inputNameEI.get(i), inputLastColumn.get(i),
                    rows.size() + i);
        }
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }

    public static void addRowToTableBill(Tbl reviewTable, Tr templateRow, String ppeName, int ppeCount,
                                         String nomNum, int price, int summa,
                                         int codeEI, String nameEI, String lastColumn,
                                         int rowSize) {
        System.out.println("ppeName - " + ppeName);
        System.out.println("rowSize - " + rowSize);
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        for (Object object : textElements) {
            Text text = (Text) object;
            if (text.getValue().equals("name")) {
                text.setValue(String.valueOf(ppeName));
            }
            if (text.getValue().equals("count")) {
                text.setValue(String.valueOf(ppeCount));
            }
            if (text.getValue().equals("nomNum")) {
                text.setValue(nomNum);
            }
            if (text.getValue().equals("price")) {
                text.setValue(String.valueOf(price));
            }
            if (text.getValue().equals("summa")) {
                text.setValue(String.valueOf(summa));
            }

            if (text.getValue().equals("codeEI")) {
                text.setValue(String.valueOf(codeEI));
            }
            if (text.getValue().equals("nameEI")) {
                text.setValue(nameEI);
            }
            if (text.getValue().equals("lastColumn")) {
                text.setValue(lastColumn);
            }
        }
        reviewTable.getContent().add(rowSize - 3, workingRow);
    }


    public static void addMeterColumn(WordprocessingMLPackage template, List<Meter> allMeters, String placeholder) throws JAXBException, Docx4JException, CloneNotSupportedException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);

        List<Object> allTC = getAllElementFromObject(template.getMainDocumentPart(), Tc.class);
       for (int i = 1; i < allMeters.size(); i++) {
        int currentTR_0 = ((Tr) rows.get(0)).getContent().size();
        int currentTR_1 = ((Tr) rows.get(1)).getContent().size();
        int currentTR_2 = ((Tr) rows.get(2)).getContent().size();


        ((Tr) rows.get(0)).getContent().add(currentTR_0, XmlUtils.deepCopy(allTC.get(1))); //вставляем meterName

        ((Tr) rows.get(1)).getContent().add(currentTR_1, XmlUtils.deepCopy(allTC.get(4)));
        ((Tr) rows.get(1)).getContent().add(currentTR_1, XmlUtils.deepCopy(allTC.get(3)));
        ((Tr) rows.get(2)).getContent().add(currentTR_2, XmlUtils.deepCopy(allTC.get(7)));
        ((Tr) rows.get(2)).getContent().add(currentTR_2, XmlUtils.deepCopy(allTC.get(6)));
        }
    }

    public static void addAuditColumn(WordprocessingMLPackage template, int lastYear, int nextYear, String placeholder) throws JAXBException, Docx4JException, CloneNotSupportedException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);

        List<Object> allTC = getAllElementFromObject(template.getMainDocumentPart(), Tc.class);
        for (int i = 1; i < lastYear; i++) {
            int currentTR_0 = ((Tr) rows.get(0)).getContent().size()-2;
            int currentTR_1 = ((Tr) rows.get(1)).getContent().size()-2;
            int currentTR_2 = ((Tr) rows.get(2)).getContent().size()-2;
            int currentTR_3 = ((Tr) rows.get(3)).getContent().size()-2;
            int currentTR_4 = ((Tr) rows.get(4)).getContent().size()-2;

            ((Tr) rows.get(0)).getContent().add(currentTR_0, XmlUtils.deepCopy(allTC.get(2)));
            ((Tr) rows.get(1)).getContent().add(currentTR_1, XmlUtils.deepCopy(allTC.get(7)));
            ((Tr) rows.get(2)).getContent().add(currentTR_2, XmlUtils.deepCopy(allTC.get(12)));
            ((Tr) rows.get(3)).getContent().add(currentTR_3, XmlUtils.deepCopy(allTC.get(17)));
            ((Tr) rows.get(4)).getContent().add(currentTR_4, XmlUtils.deepCopy(allTC.get(22)));
        }

        for (int i = 1; i < nextYear; i++) {
            int currentTR_0 = ((Tr) rows.get(0)).getContent().size();
            int currentTR_1 = ((Tr) rows.get(1)).getContent().size();
            int currentTR_2 = ((Tr) rows.get(2)).getContent().size();
            int currentTR_3 = ((Tr) rows.get(3)).getContent().size();
            int currentTR_4 = ((Tr) rows.get(4)).getContent().size();

            ((Tr) rows.get(0)).getContent().add(currentTR_0, XmlUtils.deepCopy(allTC.get(4)));
            ((Tr) rows.get(1)).getContent().add(currentTR_1, XmlUtils.deepCopy(allTC.get(9)));
            ((Tr) rows.get(2)).getContent().add(currentTR_2, XmlUtils.deepCopy(allTC.get(14)));
            ((Tr) rows.get(3)).getContent().add(currentTR_3, XmlUtils.deepCopy(allTC.get(19)));
            ((Tr) rows.get(4)).getContent().add(currentTR_4, XmlUtils.deepCopy(allTC.get(24)));
        }

    }


    //Отчет по энергопотреблению
    public static void addMeterToolRow(WordprocessingMLPackage template, List<Meter> allWaterMeters, List<Date> dateList, Set<Integer> yearsDataMeter, String placeholder) throws JAXBException, Docx4JException {
        List<Object> tables = getAllElementFromObject(template.getMainDocumentPart(), Tbl.class);
        //Поиск таблицы
        Tbl tempTable = getTemplateTable(tables, placeholder);
        List<Object> rows = getAllElementFromObject(tempTable, Tr.class);
        Tr templateRow = (Tr) rows.get(2);
        summaFactValue = 0l;
        summaPrognozValue = 0l;
        for (Date date : dateList) {
            addRowToTableMeter(tempTable, templateRow, allWaterMeters, date, yearsDataMeter);
        }
        replacePlaceholder(template, String.valueOf(summaFactValue), "fact");
        replacePlaceholder(template, String.valueOf(summaPrognozValue), "plan");
        replacePlaceholder(template, String.valueOf(summaFactValue-summaPrognozValue), "different");
        //Удаляем шаблонную строку
        tempTable.getContent().remove(templateRow);
    }


    public static void addRowToTableMeter(Tbl reviewTable, Tr templateRow, List<Meter> allWaterMeters, Date date, Set<Integer> yearsDataMeter) {
        Tr workingRow = (Tr) XmlUtils.deepCopy(templateRow);
        List textElements = getAllElementFromObject(workingRow, Text.class);
        SimpleDateFormat simpleDateFormat_out = new SimpleDateFormat("dd.MM.yyyy");
        SimpleDateFormat simpleDateFormat_week = new SimpleDateFormat("(EEEE)");
        String strDate = simpleDateFormat_out.format(date);
        String strWeek = simpleDateFormat_week.format(date);
        for (Meter meter : allWaterMeters) {
            for (Object object : textElements) {
                Text text = (Text) object;
                if (text.getValue().equals("date")) {
                    text.setValue(strDate);
                }
                if (text.getValue().equals("week")) {
                    text.setValue(strWeek);
                }
                if (text.getValue().equals("meterValue")) {
                    if (meter.getAllValueDataMeters().containsKey(date)) {
                        long value = meter.getAllValueDataMeters().get(date);
                        summaFactValue += value;
                        text.setValue(String.valueOf(value));
                    } else {
                        text.setValue("Данные не снимались");
                    }
                }
                if (text.getValue().equals("prognozValue")) {
                    long valuePrognoz = meter.getPrognoses(date, yearsDataMeter);
                    summaPrognozValue += valuePrognoz;
                    text.setValue(String.valueOf(valuePrognoz));
                    break;
                }
            }
        }
        reviewTable.getContent().add(workingRow);
    }


}