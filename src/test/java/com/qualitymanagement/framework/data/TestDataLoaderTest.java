package com.qualitymanagement.framework.data;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

public class TestDataLoaderTest {
    @Test
    public void readsCsvFieldsAndSkipsCommentRows() throws Exception {
        File csv = new File("src/test/resources/testdata/loader-sample.csv");
        List<Map<String, String>> rows = TestDataLoader.csv(csv);
        Assert.assertEquals(rows.size(), 2);
        Assert.assertEquals(rows.get(0).get("title"), "Quality, controlled copy");
        Assert.assertEquals(rows.get(0).get("notes"), "contains \"quoted\" text");
    }

    @Test
    public void readsJsonTestData() throws Exception {
        try (java.io.InputStream input = getClass().getClassLoader()
                .getResourceAsStream("testdata/documents.json")) {
            List<Map<String, Object>> rows = TestDataLoader.json(input);
            Assert.assertEquals(rows.size(), 1);
            Assert.assertEquals(rows.get(0).get("title"), "Configured test document");
        }
    }

    @Test
    public void readsExcelTestData() throws Exception {
        File workbookFile = File.createTempFile("qms-data-", ".xlsx", new File("target"));
        try {
            try (XSSFWorkbook workbook = new XSSFWorkbook();
                 FileOutputStream output = new FileOutputStream(workbookFile)) {
                org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("Cases");
                org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
                header.createCell(0).setCellValue("title");
                header.createCell(1).setCellValue("revision");
                org.apache.poi.ss.usermodel.Row data = sheet.createRow(1);
                data.createCell(0).setCellValue("Boundary case");
                data.createCell(1).setCellValue(7);
                workbook.write(output);
            }
            List<Map<String, String>> rows = TestDataLoader.excel(workbookFile);
            Assert.assertEquals(rows.size(), 1);
            Assert.assertEquals(rows.get(0).get("title"), "Boundary case");
            Assert.assertEquals(rows.get(0).get("revision"), "7");
        } finally {
            if (workbookFile.exists() && !workbookFile.delete()) {
                throw new IllegalStateException("Could not remove temporary workbook " + workbookFile);
            }
        }
    }
}
