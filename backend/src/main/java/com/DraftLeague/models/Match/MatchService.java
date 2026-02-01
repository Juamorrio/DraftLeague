package com.DraftLeague.models.Match;

import com.DraftLeague.models.Match.dto.MatchDTO;
import com.DraftLeague.models.Match.dto.UpcomingMatchDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MatchService {

    private final ObjectMapper objectMapper;

    public MatchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Map<String, List<MatchDTO>> getPlayedMatches() {
        try {
            ClassPathResource resource = new ClassPathResource("scraping/matches.json");
            InputStream inputStream = resource.getInputStream();
            TypeReference<Map<String, List<MatchDTO>>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(inputStream, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public Map<String, List<UpcomingMatchDTO>> getUpcomingMatches() {
        try {
            ClassPathResource resource = new ClassPathResource("scraping/upcoming_matches.json");
            InputStream inputStream = resource.getInputStream();
            TypeReference<Map<String, List<UpcomingMatchDTO>>> typeRef = new TypeReference<>() {};
            return objectMapper.readValue(inputStream, typeRef);
        } catch (IOException e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }

    public String syncMatches() throws Exception {
        ClassPathResource resource = new ClassPathResource("scraping/home.py");
        String scriptPath = resource.getFile().getAbsolutePath();
        
        List<String> command = new ArrayList<>();
        command.add("python");
        command.add(scriptPath);
        
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        
        Process process = processBuilder.start();
        
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        
        int exitCode = process.waitFor();
        
        if (exitCode != 0) {
            throw new RuntimeException("Error al ejecutar script de Python. Código de salida: " + exitCode + "\n" + output.toString());
        }
        
        return output.toString();
    }
}
