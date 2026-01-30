package com.monprojet.traducteur;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.apache.xmlbeans.XmlCursor;
import org.apache.xmlbeans.XmlObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

@Service
public class FileHandlerService {

    @Autowired
    private AiTranslationService translationService;

    private static final String BOM = "\uFEFF";

    // --- TXT (Inchangé) ---
    public byte[] translateTxt(MultipartFile file, String sourceLang, String targetLang) throws IOException {
        String content = new String(file.getBytes(), StandardCharsets.UTF_8);
        String[] lines = content.split("\n", -1);
        StringBuilder resultBuilder = new StringBuilder();
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.trim().isEmpty()) {
                resultBuilder.append(line);
            } else {
                String[] translationResult = translationService.translate(line, sourceLang, targetLang);
                resultBuilder.append(translationResult[0]);
            }
            if (i < lines.length - 1) {
                resultBuilder.append("\n");
            }
        }
        return (BOM + resultBuilder.toString()).getBytes(StandardCharsets.UTF_8);
    }

    // --- WORD (.docx) (Inchangé) ---
    public byte[] translateDocx(MultipartFile file, String sourceLang, String targetLang) throws IOException {
        try (InputStream is = file.getInputStream();
             XWPFDocument doc = new XWPFDocument(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            boolean isArabic = "ar".equals(targetLang);

            for (XWPFParagraph p : doc.getParagraphs()) {
                if (isArabic) forceArabicStyle(p);
            }

            scanAndTranslateXml(doc.getDocument(), sourceLang, targetLang);

            for (XWPFHeader header : doc.getHeaderList()) {
                for (XWPFParagraph p : header.getParagraphs()) {
                    translateParagraphDirectly(p, sourceLang, targetLang, isArabic);
                }
            }
            for (XWPFFooter footer : doc.getFooterList()) {
                for (XWPFParagraph p : footer.getParagraphs()) {
                    translateParagraphDirectly(p, sourceLang, targetLang, isArabic);
                }
            }

            doc.write(out);
            return out.toByteArray();
        }
    }

    // --- EXCEL (.xlsx) (NOUVEAU) ---
    public byte[] translateExcel(MultipartFile file, String sourceLang, String targetLang) throws IOException {
        try (InputStream is = file.getInputStream();
             Workbook workbook = new XSSFWorkbook(is);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            boolean isArabic = "ar".equals(targetLang);

            // Parcourir toutes les feuilles
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);

                // GESTION ARABE : Passer la feuille en mode Droite-à-Gauche
                if (isArabic) {
                    sheet.setRightToLeft(true);
                }

                // Parcourir les lignes
                for (Row row : sheet) {
                    // Parcourir les cellules
                    for (Cell cell : row) {
                        // On ne traduit que les cellules contenant du texte
                        if (cell.getCellType() == CellType.STRING) {
                            String originalText = cell.getStringCellValue();
                            
                            if (originalText != null && !originalText.trim().isEmpty()) {
                                // Traduction via le service IA existant
                                String[] result = translationService.translate(originalText, sourceLang, targetLang);
                                cell.setCellValue(result[0]);
                                
                                // Optionnel : ajuster l'alignement de la cellule pour l'arabe
                                // Attention : modifier les styles existants peut être risqué pour le formatage
                                // sheet.setRightToLeft(true) fait déjà le plus gros du travail visuel.
                            }
                        }
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    // --- Helpers Word (Inchangés) ---
    private void scanAndTranslateXml(XmlObject root, String source, String target) {
        if (root == null) return;
        XmlCursor cursor = root.newCursor();
        cursor.selectPath("declare namespace w='http://schemas.openxmlformats.org/wordprocessingml/2006/main' .//w:t");

        while (cursor.toNextSelection()) {
            XmlObject obj = cursor.getObject();
            if (obj instanceof CTText) {
                CTText ctText = (CTText) obj;
                String text = ctText.getStringValue();
                if (text != null && text.trim().length() > 0) {
                    String[] res = translationService.translate(text, source, target);
                    ctText.setStringValue(res[0]);
                }
            }
        }
        cursor.dispose();
    }

    private void translateParagraphDirectly(XWPFParagraph p, String s, String t, boolean ar) {
        String text = p.getText();
        if (text != null && !text.trim().isEmpty()) {
            String[] res = translationService.translate(text, s, t);
            if (!p.getRuns().isEmpty()) {
                p.getRuns().get(0).setText(res[0], 0);
                for (int i = p.getRuns().size() - 1; i > 0; i--) p.removeRun(i);
            }
        }
        if (ar) forceArabicStyle(p);
    }

    private void forceArabicStyle(XWPFParagraph p) {
        p.setAlignment(ParagraphAlignment.RIGHT);
        try {
            CTP ctp = p.getCTP();
            CTPPr ppr = ctp.isSetPPr() ? ctp.getPPr() : ctp.addNewPPr();
            if (!ppr.isSetBidi()) ppr.addNewBidi();
            CTJc jc = ppr.isSetJc() ? ppr.getJc() : ppr.addNewJc();
            jc.setVal(STJc.RIGHT);
        } catch (Exception e) {}
    }
}