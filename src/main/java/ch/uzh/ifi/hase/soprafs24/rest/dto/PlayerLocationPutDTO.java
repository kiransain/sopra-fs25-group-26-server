package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class PlayerLocationPutDTO {
    private double locationLat;
    private double locationLong;

    public double getLocationLat() {return locationLat; }
    public void setLocationLat(double locationLat) {this.locationLat = locationLat;}
    public double getLocationLong() {return locationLong; }
    public void setLocationLong(double locationLong) {this.locationLong = locationLong;}
}
