package com.kraftlog.pdfimport.service;

import com.kraftlog.pdfimport.config.MuscleGroupMappingConfig;
import com.kraftlog.pdfimport.dto.ParsedExerciseData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfParserService {

    private final MuscleGroupMappingConfig muscleGroupConfig;

    private static final Pattern URL_PATTERN = Pattern.compile(
            "https://(?:(?:www\\.)?youtube\\.com/watch\\?v=|youtu\\.be/)[A-Za-z0-9_-]+");

    public List<ParsedExerciseData> parseExercisesFromPdf(File pdfFile) throws IOException {
        log.info("Parsing exercises from PDF: {}", pdfFile.getName());
        
        List<ParsedExerciseData> exercises = new ArrayList<>();
        
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            
            exercises = parseExercisesFromText(text);
            
            log.info("Successfully parsed {} exercises from PDF", exercises.size());
        }
        
        return exercises;
    }

    private List<ParsedExerciseData> parseExercisesFromText(String text) {
        List<ParsedExerciseData> exercises = new ArrayList<>();
        
        String[] lines = text.split("\n");
        String currentMuscleGroup = null;
        
        for (String line : lines) {
            line = line.trim();
            
            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }
            
            // Check for muscle group header
            String detectedMuscleGroup = detectMuscleGroup(line);
            if (detectedMuscleGroup != null) {
                currentMuscleGroup = detectedMuscleGroup;
                log.debug("Found muscle group: {}", currentMuscleGroup);
                continue;
            }
            
            // Skip title, footer, and table headers
            if (line.startsWith("Vídeos dos Exercícios") ||
                line.contains("Alguns exercícios podem ter") ||
                line.contains("portanto para não haver") ||
                line.startsWith("Leandro Twin") ||
                line.startsWith("CREF:") ||
                line.startsWith("WhatsApp:") ||
                line.startsWith("www.") ||
                line.contains("Exercício") && line.contains("Execução em Vídeo") ||
                line.startsWith("Técnicas Avançadas") ||
                line.contains("Não encontrou o que queria") ||
                line.contains("forma 100% original") ||
                line.contains("Bi-set") ||
                line.contains("Agonista x Antagonista") ||
                line.contains("Alongamentos")) {
                continue;
            }
            
            // If we have a current muscle group, try to parse exercise from this line
            if (currentMuscleGroup != null) {
                ParsedExerciseData exercise = parseExerciseLine(line, currentMuscleGroup);
                if (exercise != null) {
                    exercises.add(exercise);
                    log.debug("Parsed exercise: {} | Group: {} | URL: {}", 
                            exercise.getName(), exercise.getMuscleGroupPortuguese(), exercise.getVideoUrl());
                }
            }
        }
        
        return exercises;
    }
    
    private ParsedExerciseData parseExerciseLine(String line, String muscleGroup) {
        // Extract URL if present
        Matcher urlMatcher = URL_PATTERN.matcher(line);
        String videoUrl = null;
        String exerciseName = line;
        
        if (urlMatcher.find()) {
            videoUrl = urlMatcher.group();
            // Remove URL from line to get exercise name
            exerciseName = line.substring(0, urlMatcher.start()).trim();
        }
        
        // Clean up exercise name
        exerciseName = cleanExerciseName(exerciseName);
        
        // Validate exercise name
        if (exerciseName.isEmpty() || exerciseName.length() < 3) {
            return null;
        }
        
        // Skip lines that look like sub-headers or non-exercise text
        if (isSubHeader(exerciseName)) {
            return null;
        }
        
        return ParsedExerciseData.builder()
                .name(exerciseName)
                .videoUrl(videoUrl)
                .muscleGroupPortuguese(muscleGroup)
                .build();
    }
    
    private boolean isSubHeader(String text) {
        String upper = text.toUpperCase();
        // Check if this is a sub-muscle-group header rather than an exercise
        return upper.equals("DELTÓIDES") ||
               upper.equals("TRAPÉZIO") ||
               upper.equals("ANTEBRAÇO") ||
               upper.equals("COXAS") ||
               upper.equals("PESCOÇO") ||
               upper.equals("POSTERIOR DE COXA") ||
               upper.equals("QUADRÍCEPS") ||
               upper.contains("TÉCNICAS AVANÇADAS");
    }

    private String detectMuscleGroup(String line) {
        String upperLine = line.toUpperCase();
        
        Set<String> configuredHeaders = muscleGroupConfig.getMuscleGroupHeaders();
        
        for (String header : configuredHeaders) {
            // Exact match for single-word muscle groups
            if (upperLine.equals(header.toUpperCase())) {
                return header;
            }
        }
        
        return null;
    }

    private String cleanExerciseName(String name) {
        // Remove extra whitespace
        name = name.replaceAll("\\s+", " ").trim();
        
        // Remove leading numbers, dots, and dashes
        name = name.replaceAll("^[\\d.\\-]+\\s*", "");
        
        // Remove pipes and tabs
        name = name.replaceAll("[|\\t]+", " ");
        
        // Remove backslashes used in some exercise names
        name = name.replace("\\", "/");
        
        return name.trim();
    }
}
