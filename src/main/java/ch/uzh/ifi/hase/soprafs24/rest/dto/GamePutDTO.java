package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GamePutDTO {

    private Double locationLat;
    private Double locationLong;

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
}
