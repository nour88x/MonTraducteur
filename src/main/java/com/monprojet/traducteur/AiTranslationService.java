package com.monprojet.traducteur;

import org.json.JSONArray;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class AiTranslationService {

    // Limite pour ne pas bloquer l'URL Google
    private static final int MAX_CHUNK_SIZE = 1800;

    // C'est la fonction principale appelée par Image, Voix, Texte et Fichier
    public String[] translate(String text, String langFrom, String langTo) {
        if (text == null || text.trim().isEmpty()) {
            return new String[] { "", "auto" };
        }

        // --- ETAPE 1 : RÉPARATION DU SENS (Normalisation) ---
        // C'est cette ligne qui garantit le "Bon sens" pour TOUTES les méthodes.
        // Elle recolle les phrases cassées par des retours à la ligne.
        String normalizedText = normalizeText(text);

        // --- ETAPE 2 : Découpage intelligent (si le texte est très long) ---
        List<String> chunks = splitTextSmartly(normalizedText);

        StringBuilder finalTranslation = new StringBuilder();
        String detectedLang = "auto";

        // --- ETAPE 3 : Traduction morceau par morceau ---
        for (int i = 0; i < chunks.size(); i++) {
            String chunk = chunks.get(i);
            
            String[] result = callGoogleApi(chunk, langFrom, langTo);
            
            finalTranslation.append(result[0]);
            
            // Gestion des espaces entre les morceaux
            if (i < chunks.size() - 1) {
                // On ajoute un espace seulement si le morceau ne finit pas déjà par un espace ou un saut de ligne
                if (!result[0].endsWith(" ") && !result[0].endsWith("\n")) {
                    finalTranslation.append(" ");
                }
            }

            // On garde la langue détectée du premier morceau
            if (i == 0 || (detectedLang.equals("auto") && !result[1].equals("auto"))) {
                detectedLang = result[1];
            }
        }

        return new String[] { finalTranslation.toString().trim(), detectedLang };
    }

    /**
     * CETTE FONCTION EST LA CLÉ DU "BON SENS".
     * Elle transforme : "Je suis \n content" (2 phrases) en "Je suis content" (1 phrase).
     * Elle préserve : "Paragraphe 1 \n\n Paragraphe 2" (2 paragraphes).
     */
    private String normalizeText(String text) {
        // 1. Standardiser les sauts de ligne
        String clean = text.replace("\r\n", "\n").replace("\r", "\n");

        // 2. Séparer les VRAIS paragraphes (double saut de ligne)
        String[] paragraphs = clean.split("\n\\s*\n");

        StringBuilder rebuiltText = new StringBuilder();

        for (int i = 0; i < paragraphs.length; i++) {
            String p = paragraphs[i];

            // 3. Dans un paragraphe, remplacer les sauts de ligne uniques par des espaces
            // C'est ça qui "répare" la phrase pour que Google comprenne le contexte global
            String flattendParagraph = p.replace("\n", " ");

            // 4. Enlever les doubles espaces inutiles
            flattendParagraph = flattendParagraph.replaceAll("\\s+", " ").trim();

            rebuiltText.append(flattendParagraph);

            // 5. Remettre le double saut de ligne à la fin du paragraphe
            if (i < paragraphs.length - 1) {
                rebuiltText.append("\n\n");
            }
        }
        return rebuiltText.toString();
    }

    // Appel API Google (Ne change pas)
    private String[] callGoogleApi(String text, String langFrom, String langTo) {
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String urlStr = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=" + 
                    langFrom + "&tl=" + langTo + "&dt=t&q=" + encodedText;

            URL url = new URL(urlStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
            String inputLine;
            StringBuilder response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) response.append(inputLine);
            in.close();

            JSONArray jsonArray = new JSONArray(response.toString());
            JSONArray sentences = jsonArray.getJSONArray(0);
            
            StringBuilder chunkTranslation = new StringBuilder();
            for (int i = 0; i < sentences.length(); i++) {
                chunkTranslation.append(sentences.getJSONArray(i).getString(0));
            }

            String detected = "en";
            if (jsonArray.length() > 2) {
                detected = jsonArray.getString(2);
            }
            return new String[] { chunkTranslation.toString(), detected };

        } catch (Exception e) {
            e.printStackTrace();
            return new String[] { text, langFrom };
        }
    }

    // Découpage intelligent pour les textes > 2000 caractères (Ne change pas)
    private List<String> splitTextSmartly(String text) {
        List<String> chunks = new ArrayList<>();
        int length = text.length();
        int start = 0;

        while (start < length) {
            if (length - start <= MAX_CHUNK_SIZE) {
                chunks.add(text.substring(start));
                break;
            }
            int cutIndex = start + MAX_CHUNK_SIZE;
            int bestSplit = -1;
            
            // On cherche une ponctuation forte ou un saut de ligne
            int p1 = text.lastIndexOf(". ", cutIndex);
            int p2 = text.lastIndexOf("! ", cutIndex);
            int p3 = text.lastIndexOf("? ", cutIndex);
            int p4 = text.lastIndexOf("\n", cutIndex);

            bestSplit = Math.max(p1, Math.max(p2, Math.max(p3, p4)));

            if (bestSplit <= start) {
                bestSplit = text.lastIndexOf(" ", cutIndex);
            }
            
            if (bestSplit <= start) {
                bestSplit = cutIndex;
            } else {
                bestSplit += 1;
            }

            chunks.add(text.substring(start, bestSplit));
            start = bestSplit;
        }
        return chunks;
    }
}