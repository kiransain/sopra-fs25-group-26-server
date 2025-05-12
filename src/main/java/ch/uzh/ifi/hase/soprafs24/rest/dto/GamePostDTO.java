package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GamePostDTO {

    private String gamename;
    private double radius;
    private Integer preparationTimeInSeconds;
    private Integer gameTimeInSeconds;
    private Double locationLat;
    private Double locationLong;

    public String getGamename() {
        return gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public Double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(Double locationLat) {
        this.locationLat = locationLat;
    }

    public Double getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(Double locationLong) {
        this.locationLong = locationLong;
    }

    public double getRadius() {
        return radius;
    }

    public void setRadius(double radius) {
        this.radius = radius;
    }

    public Integer getPreparationTimeInSeconds() {
        return preparationTimeInSeconds;
    }

    public void setPreparationTimeInSeconds(Integer preparationTimeInSeconds) {
        this.preparationTimeInSeconds = preparationTimeInSeconds;
    }

    public Integer getGameTimeInSeconds() {
        return gameTimeInSeconds;
    }

    public void setGameTimeInSeconds(Integer gameTimeInSeconds) {
        this.gameTimeInSeconds = gameTimeInSeconds;
    }
}
