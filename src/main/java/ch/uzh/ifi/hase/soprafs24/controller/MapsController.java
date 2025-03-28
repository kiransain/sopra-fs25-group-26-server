package ch.uzh.ifi.hase.soprafs24.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MapsController {

    @Value("${google.maps.api-key}") 
    private String apiKey;

    @GetMapping("/api/maps/key")
    public String getApiKey() {
        return apiKey;
    }
}