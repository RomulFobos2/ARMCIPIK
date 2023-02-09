package com.mai.ARMCIPIK.service;

import com.mai.ARMCIPIK.models.DataMeter;
import com.mai.ARMCIPIK.models.Meter;
import com.mai.ARMCIPIK.models.User;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MyService {
    private List<String> workOffHour= new ArrayList<>();
    {
        workOffHour.add("1:00");
        workOffHour.add("2:00");
        workOffHour.add("3:00");
        workOffHour.add("4:00");
        workOffHour.add("5:00");
        workOffHour.add("6:00");
        workOffHour.add("7:00");
        workOffHour.add("8:00");
        workOffHour.add("17:00");
        workOffHour.add("18:00");
        workOffHour.add("19:00");
        workOffHour.add("20:00");
        workOffHour.add("21:00");
        workOffHour.add("22:00");
        workOffHour.add("23:00");
        workOffHour.add("0:00");
    }
    //Получаю код дня. 0 - рабочий, 1 - не рабочий
    public static int isDayOff(Date date) throws IOException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYYMMdd");
        System.out.println(simpleDateFormat.format(date));
        String url = "https://isdayoff.ru/" + simpleDateFormat.format(date);

        URL obj = new URL(url);
        HttpURLConnection connection = (HttpURLConnection) obj.openConnection();

        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        return Integer.parseInt(response.toString());
    }

    public static Date getWorkDate(Date date) throws IOException {
        int result = isDayOff(date);
        while (result != 0) {
            date.setDate(date.getDate() - 1);
            result = isDayOff(date);
        }
        return date;
    }

    //Получаем список дат в промежутке от dateFrom до dateTo
    public static List<Date> getDateList(Date dateFrom, Date dateTo) throws IOException {
        List<Date> result = new ArrayList<>();
        Date varDate = new Date(dateFrom.getTime());
        while (varDate.getTime() / 1000 < (dateTo.getTime() / 1000)) {
            Date newDate = new Date(varDate.getTime());
            result.add(newDate);
            varDate.setDate(varDate.getDate() + 1);
        }
        return result;
    }

    //Получаем список месяцев в промежутке от dateFrom до dateTo
    public static List<Date> getMonthList(Date dateFrom, Date dateTo) throws IOException {
        List<Date> result = new ArrayList<>();
        Date varDate = new Date(dateFrom.getTime());
        while (varDate.getMonth() <= dateTo.getMonth() & varDate.getYear() <= dateTo.getYear()) {
            Date newDate = new Date(varDate.getTime());
            result.add(newDate);
            varDate.setMonth(varDate.getMonth() + 1);
            System.out.println("Новый месяц получаем");
        }

        System.out.println("Полученый список месяев - " + result);
        return result;
    }

    public static String getDayOfWeek(int index) {
        switch (index) {
            case 0:
                return "Понедельник";
            case 1:
                return "Вторник";
            case 2:
                return "Среда";
            case 3:
                return "Четверг";
            case 4:
                return "Пятница";
            case 5:
                return "Суббота";
            case 6:
                return "Воскресенье";
            default:
                return "Вся неделя";
        }
    }

    public String getMonthName(Date date) {
        int countMonth = date.getMonth();
        switch (countMonth) {
            case 0:
                return "Январь";
            case 1:
                return "Февраль";
            case 2:
                return "Март";
            case 3:
                return "Апрель";
            case 4:
                return "Май";
            case 5:
                return "Июнь";
            case 6:
                return "Июль";
            case 7:
                return "Август";
            case 8:
                return "Сентябрь";
            case 9:
                return "Октябрь";
            case 10:
                return "Ноябрь";
            default:
                return "Декабрь";
        }
    }

    public static String getMonthNameStatic(Date date) {
        int countMonth = date.getMonth();
        switch (countMonth) {
            case 0:
                return "Январь";
            case 1:
                return "Февраль";
            case 2:
                return "Март";
            case 3:
                return "Апрель";
            case 4:
                return "Май";
            case 5:
                return "Июнь";
            case 6:
                return "Июль";
            case 7:
                return "Август";
            case 8:
                return "Сентябрь";
            case 9:
                return "Октябрь";
            case 10:
                return "Ноябрь";
            default:
                return "Декабрь";
        }
    }

    public static int getDayOfWeek(String day) {
        switch (day) {
            case "понедельник":
                return 0;
            case "вторник":
                return 1;
            case "среда":
                return 2;
            case "четверг":
                return 3;
            case "пятница":
                return 4;
            case "суббота":
                return 5;
            case "воскресенье":
                return 6;
            default:
                return -1;
        }
    }

    //ЧТоб на странице addWeekRequest не вручную писать время, создаю массив и генерирую инпуты через него
    public static List<String> listTimeForWeekRequest() {
        List<String> result = new ArrayList<>();
        for (int i = 1; i < 24; i++) {
            result.add(i + ":00");
        }
        result.add("0:00");
        return result;
    }

    //Чекаем час, рабочий или не рабочий
    public boolean getInfoAboutHour(String hour) {
        return workOffHour.contains(hour);
    }


    public static Set<DataMeter> parse(Sheet sheet, String meterName, HSSFWorkbook wb, Meter meter, User user) {
        Set<DataMeter> dataMeterHashSet = new HashSet<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyг.");
        SimpleDateFormat dateFormatWeek = new SimpleDateFormat("EEEE");
        ArrayList<Date> allDates = new ArrayList<>();
        ArrayList<String> allValues = new ArrayList<>();
        Iterator<Row> it = sheet.iterator();
        while (it.hasNext()) {
            Row row = it.next();
            Iterator<Cell> cells = row.iterator();
            boolean check = false;
            while (cells.hasNext()) {
                Cell cell = cells.next();
                CellType cellType = cell.getCellType();
                switch (cellType) {
                    case BLANK:
                        if (check) {
                            allValues.add("ПУСТО");
                        }
                        break;
                    case FORMULA:
                        if (check) {
                            FormulaEvaluator evaluator = wb.getCreationHelper().createFormulaEvaluator();
                            allValues.add(String.valueOf(evaluator.evaluate(cell).getNumberValue()));
                        }
                        break;
                    case NUMERIC:
                        if (check) {
                            allValues.add(String.valueOf(cell.getNumericCellValue()));
                        }
                        break;
                    case STRING:
                        try {
                            allDates.add(dateFormat.parse(cell.getStringCellValue()));
                        } catch (ParseException e) {
                            if (cell.getStringCellValue().equals(meterName)) {
                                check = true;
                            } else {
                                if (check) {
                                    try {
                                        float newValue = Float.parseFloat(cell.getStringCellValue());
                                        allValues.add(String.valueOf(newValue));
                                    } catch (NumberFormatException floatException) {
                                        check = false;
                                    }
                                }
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        System.out.println("allDates.size() - " + allDates.size());
        System.out.println("allValues.size() - " + allValues.size());
        if(allDates.size() == allValues.size()){
            for(int i = 0; i < allDates.size(); i++){
                if(allValues.get(i).equals("ПУСТО")){
                    continue;
                }
                try {
                    Date date = allDates.get(i);
                    float f = Float.parseFloat(allValues.get(i));
                    long valueMeter = (long) f;
                    int indexDay = MyService.getDayOfWeek(dateFormatWeek.format(date));
                    DataMeter dataMeter = new DataMeter(date, indexDay, dateFormatWeek.format(date), meter);
                    dataMeter.setValueMeter(valueMeter);
                    dataMeter.setUser(user);
                    dataMeterHashSet.add(dataMeter);
                }
                catch (Exception longParseException){
                    longParseException.printStackTrace();
                }
            }
        }
        System.out.println("dataMeterHashSet.size() - " + dataMeterHashSet.size());
        return  dataMeterHashSet;
    }
}
