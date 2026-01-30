package com.monprojet.traducteur;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TranslationController {

    @Autowired
    private AiTranslationService translationService;

    @Autowired
    private FileHandlerService fileHandlerService;

    @PostMapping("/translate-text")
    public TranslateResponse translateText(@RequestBody TranslateRequest request) {
        String[] result = translationService.translate(
                request.getText(),
                request.getSourceLang(),
                request.getTargetLang()
        );
        return new TranslateResponse(result[0], result[1]);
    }

    @PostMapping("/translate-image")
    public TranslateResponse translateImage(
            @RequestParam("image") MultipartFile image,
            @RequestParam("sourceLang") String sourceLang,
            @RequestParam("targetLang") String targetLang) {
        try {
            Tesseract tesseract = new Tesseract();

            File linuxTessData = new File("/usr/share/tesseract-ocr/4.00/tessdata");
            if (!linuxTessData.exists()) {
                linuxTessData = new File("/usr/share/tesseract-ocr/tessdata");
            }

            if (linuxTessData.exists()) {
                tesseract.setDatapath(linuxTessData.getAbsolutePath());
            } else {
                tesseract.setDatapath("C:\\Program Files\\Tesseract-OCR\\tessdata");
            }

            String ocrLang;
            if ("auto".equals(sourceLang)) {
                ocrLang = "eng+fra+ara+spa+tur";
            } else {
                ocrLang = mapLangToTesseract(sourceLang);
            }
            tesseract.setLanguage(ocrLang);
            tesseract.setPageSegMode(3);

            String originalFilename = image.getOriginalFilename();
            String ext = ".tmp";
            if (originalFilename != null && originalFilename.contains(".")) {
                ext = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            File rawFile = File.createTempFile("ocr-raw-", ext);
            image.transferTo(rawFile);

            String textFromImage = null;
            try {
                textFromImage = tesseract.doOCR(rawFile);
            } catch (Exception e) {
                System.err.println("Erreur OCR : " + e.getMessage());
            }

            rawFile.delete();

            if (textFromImage != null) {
                textFromImage = textFromImage.replaceAll("[^\\p{L}\\p{N}\\p{P}\\s]", "");
                textFromImage = textFromImage.replaceAll("\\s+", " ").trim();
            }

            if (textFromImage == null || textFromImage.trim().isEmpty()) {
                return new TranslateResponse("Aucun texte détecté ou image non supportée", "", sourceLang);
            }

            String[] result = translationService.translate(
                    textFromImage,
                    sourceLang.equals("auto") ? "auto" : sourceLang,
                    targetLang
            );

            return new TranslateResponse(result[0], textFromImage, result[1]);

        } catch (Exception e) {
            return new TranslateResponse("Erreur OCR: " + e.getMessage());
        }
    }

    @PostMapping("/translate-file")
    public ResponseEntity<InputStreamResource> translateFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLang") String sourceLang,
            @RequestParam("targetLang") String targetLang) {
        try {
            String filename = file.getOriginalFilename().toLowerCase();
            byte[] translatedBytes;
            String contentType;
            String outFilename;

            if (filename.endsWith(".docx")) {
                translatedBytes = fileHandlerService.translateDocx(file, sourceLang, targetLang);
                contentType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
                outFilename = "traduction.docx";
            } 
            // REMPLACEMENT PDF PAR EXCEL
            else if (filename.endsWith(".xlsx") || filename.endsWith(".xls")) {
                translatedBytes = fileHandlerService.translateExcel(file, sourceLang, targetLang);
                contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                outFilename = "traduction.xlsx";
            } 
            else {
                translatedBytes = fileHandlerService.translateTxt(file, sourceLang, targetLang);
                contentType = MediaType.TEXT_PLAIN_VALUE;
                outFilename = "traduction.txt";
            }

            ByteArrayInputStream bis = new ByteArrayInputStream(translatedBytes);
            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Disposition", "attachment; filename=" + outFilename);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(new InputStreamResource(bis));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String mapLangToTesseract(String lang) {
        switch (lang) {
            case "fr": return "fra";
            case "ar": return "ara";
            case "es": return "spa";
            case "tr": return "tur";
            default: return "eng";
        }
    }
}

class TranslateRequest {
    private String text, sourceLang, targetLang;
    public String getText() { return text; }
    public String getSourceLang() { return sourceLang; }
    public String getTargetLang() { return targetLang; }
    public void setText(String text) { this.text = text; }
    public void setSourceLang(String sourceLang) { this.sourceLang = sourceLang; }
    public void setTargetLang(String targetLang) { this.targetLang = targetLang; }
}

class TranslateResponse {
    public String translatedText;
    public String detectedSourceText;
    public String detectedSourceLang;

    public TranslateResponse(String t) {
        this.translatedText = t;
    }

    public TranslateResponse(String t, String s) {
        this.translatedText = t;
        this.detectedSourceText = s;
    }

    public TranslateResponse(String t, String s, String lang) {
        this.translatedText = t;
        this.detectedSourceText = s;
        this.detectedSourceLang = lang;
    }
}