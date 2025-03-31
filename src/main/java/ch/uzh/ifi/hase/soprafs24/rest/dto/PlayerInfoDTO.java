package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;



public class PlayerInfoDTO {

    private Long playerId;
    private Long userId;
    private String username;
    private PlayerRole role;
    private PlayerStatus status;
    private double locationLatitude;
    private double locationLongitude;


    public Long getPlayerId() { return playerId; }

    public void setPlayerId(Long playerId) { this.playerId = playerId; }

    public Long getUserId() { return userId; }

    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }

    public void setUsername(String username) { this.username = username; }

    public PlayerRole getRole() { return role; }

    public void setRole(PlayerRole role) { this.role = role; }

    public PlayerStatus getStatus() { return status; }

    public void setStatus(PlayerStatus status) { this.status = status; }

    public double getLocationLatitude() { return this.locationLatitude; }

    public void setLocationLatitude(double locationLatitude) { this.locationLatitude = locationLatitude; }

    public double getLocationLongitude() { return this.locationLongitude; }

    public void setLocationLongitude(double locationLongitude) { this.locationLongitude = locationLongitude; }
}