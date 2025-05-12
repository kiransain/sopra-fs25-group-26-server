package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;

import java.time.LocalDateTime;
import java.util.List;

public class GameGetDTO {

    private Long gameId;
    private String gamename;
    private GameStatus status;
    private Double centerLatitude;
    private Double centerLongitude;
    private Integer preparationTimeInSeconds;
    private Integer gameTimeInSeconds;
    private LocalDateTime timer;
    private Double radius;
    private Long creatorId; // ID of the player (not User)
    private List<PlayerGetDTO> players;

    public Long getGameId() {
        return gameId;
    }

    public void setGameId(Long gameId) {
        this.gameId = gameId;
    }

    public String getGamename() {
        return gamename;
    }

    public void setGamename(String gamename) {
        this.gamename = gamename;
    }

    public GameStatus getStatus() {
        return status;
    }

    public void setStatus(GameStatus status) {
        this.status = status;
    }

    public Double getCenterLatitude() {
        return centerLatitude;
    }

    public void setCenterLatitude(Double centerLatitude) {
        this.centerLatitude = centerLatitude;
    }

    public Double getCenterLongitude() {
        return centerLongitude;
    }

    public void setCenterLongitude(Double centerLongitude) {
        this.centerLongitude = centerLongitude;
    }

    public Double getRadius() {
        return radius;
    }

    public void setRadius(Double radius) {
        this.radius = radius;
    }

    public Long getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(Long creatorId) {
        this.creatorId = creatorId;
    }

    public List<PlayerGetDTO> getPlayers() {
        return players;
    }

    public void setPlayers(List<PlayerGetDTO> players) {
        this.players = players;
    }

    public LocalDateTime getTimer() {
        return timer;
    }

    public void setTimer(LocalDateTime timer) {
        this.timer = timer;
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
