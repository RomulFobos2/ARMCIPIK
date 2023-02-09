package com.mai.ARMCIPIK;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Parser {
    public static void main(String[] args) {
//        parse("Data2.xls", "ГРЩТ-1");
//        parse("Data2.xls", "ГРЩТ-2");
//        parse("Data2.xls", "ГРЩ-1");
        parse("Data2.xls", "ГРЩ-2");
    }
    public static String parse(String name, String meterName) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyг.");
        ArrayList<Date> allDates = new ArrayList<>();
        ArrayList<String> allValues = new ArrayList<>();
        ArrayList<Integer> countColumns = new ArrayList<>();
        int countCheck = 0;
        String result = "";
        InputStream in = null;
        HSSFWorkbook wb = null;
        try {
            in = new FileInputStream(name);
            wb = new HSSFWorkbook(in);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Кол-во листов = " + wb.getNumberOfSheets());
        Sheet sheet = wb.getSheetAt(0);
//        Sheet sheet = wb.getSheetAt(1);
//        Sheet sheet = wb.getSheetAt(2);
//        Sheet sheet = wb.getSheetAt(3);
//        Sheet sheet = wb.getSheetAt(4);
//        Sheet sheet = wb.getSheetAt(5);
        Iterator<Row> it = sheet.iterator();
        while (it.hasNext()) {
            Row row = it.next();
            Iterator<Cell> cells = row.iterator();
            boolean check = false;
            int countColumn = 0;
            while (cells.hasNext()) {
                countColumn++;
                Cell cell = cells.next();
                CellType cellType = cell.getCellType();
                switch (cellType) {
                    case _NONE:
                        System.out.print("");
                        System.out.print("\t");
                        System.out.println("Ячейка _NONE");
                        break;
                    case BOOLEAN:
                        System.out.print(cell.getBooleanCellValue());
                        System.out.print("\t");
                        System.out.println("Ячейка BOOLEAN");
                        break;
                    case BLANK:
                        System.out.print("");
                        System.out.print("\t");
                        System.out.println("Ячейка BLANK");
                        if(check){
                            allValues.add("ПУСТО");
                            System.out.println("Добавил BLANK");
                        }
                        break;
                    case FORMULA:
                        // Formula
//                        System.out.print("cell.getCellFormula() - " + cell.getCellFormula());
                        FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                        // Print out value evaluated by formula
                        System.out.print(evaluator.evaluate(cell).getNumberValue());
                        System.out.print("\t");
                        System.out.println("Ячейка FORMULA");
                        if(check){
                            allValues.add(String.valueOf(evaluator.evaluate(cell).getNumberValue()));
                        }
                        break;
                    case NUMERIC:
                        System.out.print(cell.getNumericCellValue());
                        System.out.print("\t");
                        System.out.println("Ячейка NUMERIC");
                        if(check){
                            allValues.add(String.valueOf(cell.getNumericCellValue()));
                        }
                        break;
                    case STRING:
                        try {
                            allDates.add(dateFormat.parse(cell.getStringCellValue()));
                            System.out.print(cell.getStringCellValue() + " : ");
                            System.out.print(dateFormat.parse(cell.getStringCellValue()));
                            System.out.print("\t");
                            System.out.println("Ячейка Дата");
                        } catch (ParseException e) {
                            System.out.print(cell.getStringCellValue());
                            System.out.print("\t");
                            System.out.println("Ячейка STRING");
                            if(cell.getStringCellValue().equals(meterName)){
                                countCheck++;
                                System.out.println("Увеличиваем countCheck = " + countCheck);
                                check = true;
                            }
                            else {
                                if(check){
                                    try {
                                        float newValue = Float.parseFloat(cell.getStringCellValue());
                                        allValues.add(String.valueOf(newValue));
                                        System.out.println("newValue = " + newValue);
                                    }
                                    catch (NumberFormatException floatException){
                                        check = false;
                                    }
                                }
                            }
                        }
                        break;
                    case ERROR:
                        System.out.print("!");
                        System.out.print("\t");
                        System.out.println("Ячейка ERROR");
                        break;
                    default:
                        System.out.println("default");
                }
            }
            if(check){
                System.out.println("countCheck = " + countCheck);
                countColumns.add(countColumn);
            }
            result += "\n";
        }
        System.out.println("countCheck = " + countCheck);
        System.out.println("countColumns.size() = " + countColumns.size());
        System.out.println("countColumns = " + countColumns);
        System.out.println("allDates.size() - " + allDates.size());
        System.out.println("allValues.size() - " + allValues.size());
        return result;
    }

}
