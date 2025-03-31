package ch.uzh.ifi.hase.soprafs24.rest.dto;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;


import java.util.List;

// DTO for sending game information to the client
public class GameGetDTO {

    private Long gameId;
    private String gameName;
    private GameStatus status;
    private List<PlayerInfoDTO> players;
    private String creationDate;
    private Double centerLatitude;
    private Double centerLongitude;
    private Double radius;

    public Long getGameId() { return gameId; }

    public void setGameId(Long gameId) { this.gameId = gameId; }

    public String getGameName() { return gameName; }

    public void setGameName(String gameName) { this.gameName = gameName; }

    public GameStatus getStatus() { return status; }

    public void setStatus(GameStatus status) { this.status = status; }

    public List<PlayerInfoDTO> getPlayers() { return players; }

    public void setPlayers(List<PlayerInfoDTO> players) { this.players = players; }

    public String getCreationDate() { return creationDate; }

    public void setCreationDate(String creationDate) { this.creationDate = creationDate; }

    public Double getCenterLatitude() { return centerLatitude; }

    public void setCenterLatitude(Double centerLatitude) { this.centerLatitude = centerLatitude; }

    public Double getCenterLongitude() { return centerLongitude; }

    public void setCenterLongitude(Double centerLongitude) { this.centerLongitude = centerLongitude; }

    public Double getRadius() { return radius; }

    public void setRadius(Double radius) { this.radius = radius; }
}