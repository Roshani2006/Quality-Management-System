package com.qualitymanagement.framework.data;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestDataLoader {
    private static final ObjectMapper JSON = new ObjectMapper();

    private TestDataLoader() {
    }

    public static List<Map<String, String>> csv(File file) throws IOException {
        List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        List<String> headers = null;
        for (String line : lines) {
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            List<String> values = parseCsvLine(line);
            if (headers == null) {
                headers = values;
                continue;
            }
            if (values.size() != headers.size()) {
                throw new IOException("CSV row has " + values.size() + " fields; expected "
                        + headers.size() + ": " + line);
            }
            Map<String, String> row = new LinkedHashMap<String, String>();
            for (int i = 0; i < headers.size(); i++) {
                row.put(headers.get(i), values.get(i));
            }
            rows.add(row);
        }
        if (headers == null) {
            throw new IOException("CSV file has no header row: " + file);
        }
        return rows;
    }

    public static List<Map<String, Object>> json(InputStream input) throws IOException {
        return JSON.readValue(input, new TypeReference<List<Map<String, Object>>>() { });
    }

    public static List<Map<String, String>> excel(File file) throws IOException {
        List<Map<String, String>> rows = new ArrayList<Map<String, String>>();
        try (Workbook workbook = WorkbookFactory.create(file)) {
            Sheet sheet = workbook.getSheetAt(0);
            Row headerRow = sheet.getRow(sheet.getFirstRowNum());
            if (headerRow == null) {
                throw new IOException("Excel sheet has no header row: " + file);
            }
            DataFormatter formatter = new DataFormatter();
            List<String> headers = new ArrayList<String>();
            for (int column = 0; column < headerRow.getLastCellNum(); column++) {
                headers.add(formatter.formatCellValue(headerRow.getCell(column)).trim());
            }
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row dataRow = sheet.getRow(rowIndex);
                if (dataRow == null) {
                    continue;
                }
                Map<String, String> row = new LinkedHashMap<String, String>();
                for (int column = 0; column < headers.size(); column++) {
                    row.put(headers.get(column), formatter.formatCellValue(dataRow.getCell(column)).trim());
                }
                rows.add(row);
            }
        } catch (RuntimeException e) {
            throw new IOException("Could not read Excel test data: " + file, e);
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) throws IOException {
        List<String> values = new ArrayList<String>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (current == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    value.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (current == ',' && !quoted) {
                values.add(value.toString().trim());
                value.setLength(0);
            } else {
                value.append(current);
            }
        }
        if (quoted) {
            throw new IOException("Unterminated quoted value in CSV row: " + line);
        }
        values.add(value.toString().trim());
        return values;
    }
}
