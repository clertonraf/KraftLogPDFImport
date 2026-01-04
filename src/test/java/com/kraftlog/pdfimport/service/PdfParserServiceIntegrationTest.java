package com.kraftlog.pdfimport.service;

import com.kraftlog.pdfimport.dto.ParsedExerciseData;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestPropertySource(properties = {
    "kraftlog.muscle-groups.config-path=exercise-muscle-groups.yml"
})
class PdfParserServiceIntegrationTest {

    @Autowired
    private PdfParserService pdfParserService;

    @Test
    void testParseRealPdfFile() throws Exception {
        File pdfFile = new File("tmp/lista-de-videos-de-exercicios.pdf");
        
        if (!pdfFile.exists()) {
            System.out.println("Skipping test: PDF file not found");
            return;
        }
        
        List<ParsedExerciseData> exercises = pdfParserService.parseExercisesFromPdf(pdfFile);
        
        // Should find a substantial number of exercises (at least 200)
        assertTrue(exercises.size() >= 200, 
                "Should parse at least 200 exercises, got: " + exercises.size());
        
        // Check that all exercises have names
        for (ParsedExerciseData exercise : exercises) {
            assertNotNull(exercise.getName());
            assertFalse(exercise.getName().isEmpty());
            assertTrue(exercise.getName().length() >= 3);
        }
        
        // Check that exercises are distributed across muscle groups
        Map<String, Long> muscleGroupCounts = exercises.stream()
                .filter(ex -> ex.getMuscleGroupPortuguese() != null)
                .collect(Collectors.groupingBy(
                    ex -> ex.getMuscleGroupPortuguese(),
                    Collectors.counting()
                ));
        
        System.out.println("\n=== Exercise Distribution by Muscle Group ===");
        muscleGroupCounts.forEach((group, count) -> 
            System.out.println(group + ": " + count + " exercises")
        );
        
        // Should have multiple muscle groups
        assertTrue(muscleGroupCounts.size() >= 3);
        
        // Check specific muscle groups exist
        assertTrue(muscleGroupCounts.containsKey("PEITORAL"));
        assertTrue(muscleGroupCounts.containsKey("DORSAIS"));
        
        // Most exercises should have video URLs
        long exercisesWithUrls = exercises.stream()
                .filter(ex -> ex.getVideoUrl() != null && !ex.getVideoUrl().isEmpty())
                .count();
        
        double urlPercentage = (exercisesWithUrls * 100.0) / exercises.size();
        System.out.println("\nExercises with URLs: " + exercisesWithUrls + 
                " (" + String.format("%.1f", urlPercentage) + "%)");
        
        assertTrue(urlPercentage >= 80);
        
        System.out.println("\n✅ Successfully parsed " + exercises.size() + " exercises!");
    }
    
    @Test
    void testParseSpecificExercises() throws Exception {
        File pdfFile = new File("tmp/lista-de-videos-de-exercicios.pdf");
        
        if (!pdfFile.exists()) {
            return;
        }
        
        List<ParsedExerciseData> exercises = pdfParserService.parseExercisesFromPdf(pdfFile);
        
        // Check for specific known exercises
        assertTrue(exercises.stream().anyMatch(ex -> 
            ex.getName().contains("Supino Reto Barra")));
            
        assertTrue(exercises.stream().anyMatch(ex -> 
            ex.getName().contains("Levantamento Terra")));
            
        // Check that muscle groups are correctly assigned
        ParsedExerciseData supinoExercise = exercises.stream()
            .filter(ex -> ex.getName().contains("Supino Reto Barra"))
            .findFirst()
            .orElse(null);
            
        assertNotNull(supinoExercise);
        assertEquals("PEITORAL", supinoExercise.getMuscleGroupPortuguese());
        assertNotNull(supinoExercise.getVideoUrl());
    }
}
