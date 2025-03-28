package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;
import java.util.Collections;
import org.springframework.web.bind.annotation.CrossOrigin;


@RestController
@CrossOrigin(origins = "*")
public class MapsController {

    @Value("${google.maps.api-key}") 
    private String apiKey;

    @GetMapping("/api/maps/key")
    public Map<String, String> getApiKey() {
    return Collections.singletonMap("apiKey", apiKey);
    }
}