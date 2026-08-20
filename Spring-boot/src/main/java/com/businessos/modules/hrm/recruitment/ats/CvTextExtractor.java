package com.businessos.modules.hrm.recruitment.ats;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;

/** Plain text extraction from an uploaded resume file - PDF via PDFBox, DOCX via POI. */
@Component
public class CvTextExtractor {

    public String extract(byte[] bytes, String extension) throws UnsupportedResumeFormatException, IOException {
        String ext = extension == null ? "" : extension.toLowerCase();
        return switch (ext) {
            case ".pdf" -> extractPdf(bytes);
            case ".docx" -> extractDocx(bytes);
            default -> throw new UnsupportedResumeFormatException(ext);
        };
    }

    private String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes));
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
