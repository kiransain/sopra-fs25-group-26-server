package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class GamePostDTO {
    private String gameName;
    private Long creatorUserId;


    public String getGameName() { return gameName; }

    public void setGameName(String gameName) { this.gameName = gameName; }

    public Long getCreatorUserId() { return creatorUserId; }

    public void setCreatorUserId(Long creatorUserId) { this.creatorUserId = creatorUserId; }
}
