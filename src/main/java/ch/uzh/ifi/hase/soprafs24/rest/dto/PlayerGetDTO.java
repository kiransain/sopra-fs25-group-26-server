package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;

import java.time.LocalDateTime;

public class PlayerGetDTO {

    private Long playerId;
    private Long userId;
    private PlayerRole role;
    private PlayerStatus status;
    private boolean outOfArea;
    private LocalDateTime foundTime;
    private Double locationLat;
    private Double locationLong;
    private Double rank;

    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public PlayerRole getRole() {
        return role;
    }

    public void setRole(PlayerRole role) {
        this.role = role;
    }

    public PlayerStatus getStatus() {
        return status;
    }

    public void setStatus(PlayerStatus status) {
        this.status = status;
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

    public boolean isOutOfArea() {
        return outOfArea;
    }

    public void setOutOfArea(boolean outOfArea) {
        this.outOfArea = outOfArea;
    }

    public LocalDateTime getFoundTime() {
        return foundTime;
    }

    public void setFoundTime(LocalDateTime foundTime) {
        this.foundTime = foundTime;
    }

    public Double getRank() {
        return rank;
    }

    public void setRank(Double rank) {
        this.rank = rank;
    }
}
