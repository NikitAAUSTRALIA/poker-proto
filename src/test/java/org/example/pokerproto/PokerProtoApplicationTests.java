package org.example.pokerproto;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class PokerProtoApplicationTests {

    @Test
    void generateDemoExcel() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_80_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            Sheet sheet1 = workbook.createSheet("Входы");
            String[] h1 = {"Игрок", "Telegram ID", "Никнейм", "Статус", "Фишки"};
            Row r0 = sheet1.createRow(0);
            for (int i = 0; i < h1.length; i++) {
                Cell c = r0.createCell(i);
                c.setCellValue(h1[i]);
                c.setCellStyle(headerStyle);
            }
            String[][] p1 = {
                    {"Александр Смирнов", "1001", "@smirnov", "В игре", "50000"},
                    {"Михаил Иванов", "1002", "@misha_iv", "В игре", "50000"},
                    {"Дмитрий Кузнецов", "1003", "@dmitry_k", "В игре", "50000"},
                    {"Сергей Попов", "1004", "@spopov", "В игре", "50000"},
                    {"Андрей Васильев", "1005", "@andrey_v", "В игре", "50000"},
                    {"Алексей Соколов", "1006", "@alex_sok", "В игре", "50000"},
                    {"Максим Новиков", "1007", "@max_nov", "В игре", "50000"},
                    {"Иван Морозов", "1008", "@morozov_i", "В игре", "50000"},
                    {"Артем Федоров", "1009", "@artem_fed", "Выбыл", "50000"},
                    {"Роман Волков", "1010", "@r_volkov", "Выбыл", "50000"},
                    {"Егор Лебедев", "1011", "@lebedev_e", "Не явился", "0"},
                    {"Никита Козлов", "1012", "@kozlov_n", "Ожидается", "50000"}
            };
            for (int i = 0; i < p1.length; i++) {
                Row row = sheet1.createRow(i + 1);
                row.createCell(0).setCellValue(p1[i][0]);
                row.createCell(1).setCellValue(Long.parseLong(p1[i][1]));
                row.createCell(2).setCellValue(p1[i][2]);
                row.createCell(3).setCellValue(p1[i][3]);
                row.createCell(4).setCellValue(Double.parseDouble(p1[i][4]));
            }

            Sheet sheet2 = workbook.createSheet("Ребаи");
            String[] h2 = {"Игрок", "Telegram ID", "Никнейм", "Сумма фишек"};
            Row r1 = sheet2.createRow(0);
            for (int i = 0; i < h2.length; i++) {
                Cell c = r1.createCell(i);
                c.setCellValue(h2[i]);
                c.setCellStyle(headerStyle);
            }
            String[][] p2 = {
                    {"Александр Смирнов", "1001", "@smirnov", "50000"},
                    {"Михаил Иванов", "1002", "@misha_iv", "50000"},
                    {"Сергей Попов", "1004", "@spopov", "50000"},
                    {"Александр Смирнов", "1001", "@smirnov", "50000"}
            };
            for (int i = 0; i < p2.length; i++) {
                Row row = sheet2.createRow(i + 1);
                row.createCell(0).setCellValue(p2[i][0]);
                row.createCell(1).setCellValue(Long.parseLong(p2[i][1]));
                row.createCell(2).setCellValue(p2[i][2]);
                row.createCell(3).setCellValue(Double.parseDouble(p2[i][3]));
            }

            Sheet sheet3 = workbook.createSheet("Лидеры");
            String[] h3 = {"Игрок", "Telegram ID", "Никнейм", "Фишки"};
            Row r2 = sheet3.createRow(0);
            for (int i = 0; i < h3.length; i++) {
                Cell c = r2.createCell(i);
                c.setCellValue(h3[i]);
                c.setCellStyle(headerStyle);
            }
            String[][] p3 = {
                    {"Александр Смирнов", "1001", "@smirnov", "240000"},
                    {"Михаил Иванов", "1002", "@misha_iv", "185000"},
                    {"Сергей Попов", "1004", "@spopov", "130000"},
                    {"Дмитрий Кузнецов", "1003", "@dmitry_k", "95000"},
                    {"Андрей Васильев", "1005", "@andrey_v", "75000"}
            };
            for (int i = 0; i < p3.length; i++) {
                Row row = sheet3.createRow(i + 1);
                row.createCell(0).setCellValue(p3[i][0]);
                row.createCell(1).setCellValue(Long.parseLong(p3[i][1]));
                row.createCell(2).setCellValue(p3[i][2]);
                row.createCell(3).setCellValue(Double.parseDouble(p3[i][3]));
            }

            Sheet sheet4 = workbook.createSheet("Выбывшие");
            String[] h4 = {"Игрок", "Telegram ID", "Никнейм", "Время"};
            Row r3 = sheet4.createRow(0);
            for (int i = 0; i < h4.length; i++) {
                Cell c = r3.createCell(i);
                c.setCellValue(h4[i]);
                c.setCellStyle(headerStyle);
            }
            String[][] p4 = {
                    {"Артем Федоров", "1009", "@artem_fed", "14:15"},
                    {"Роман Волков", "1010", "@r_volkov", "14:40"}
            };
            for (int i = 0; i < p4.length; i++) {
                Row row = sheet4.createRow(i + 1);
                row.createCell(0).setCellValue(p4[i][0]);
                row.createCell(1).setCellValue(Long.parseLong(p4[i][1]));
                row.createCell(2).setCellValue(p4[i][2]);
                row.createCell(3).setCellValue(p4[i][3]);
            }

            for (Sheet s : List.of(sheet1, sheet2, sheet3, sheet4)) {
                for (int c = 0; c < 5; c++) {
                    s.autoSizeColumn(c);
                    s.setColumnWidth(c, Math.max(s.getColumnWidth(c), 4000));
                }
            }

            Path p1Path = Paths.get("src/main/resources/static/demo_tournament.xlsx");
            try (FileOutputStream fos = new FileOutputStream(p1Path.toFile())) {
                workbook.write(fos);
            }

            Path p2Path = Paths.get("src/main/resources/static/test_tv_board.xlsx");
            try (FileOutputStream fos = new FileOutputStream(p2Path.toFile())) {
                workbook.write(fos);
            }
        }
    }
}
