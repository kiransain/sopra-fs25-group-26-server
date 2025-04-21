package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "PLAYER")
public class Player {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long playerId;

    //many player can belong to one game
    @ManyToOne
    @JoinColumn(name = "game_id")
    private Game game;

    //one player can only belong to one user and two players can not link to same user
    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column
    private String displayName;

    @Column()
    private PlayerStatus status;

    @Column()
    private PlayerRole role;

    @Column()
    private double locationLat;

    @Column()
    private double locationLong;

    @Column
    private boolean outOfArea;

    @Column
    private LocalDateTime foundTime;

    @Column
    private double rank;


    public Long getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Long playerId) {
        this.playerId = playerId;
    }

    public Game getGame() {
        return game;
    }

    public void setGame(Game game) {
        this.game = game;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
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

    public double getLocationLat() {
        return locationLat;
    }

    public void setLocationLat(double locationLat) {
        this.locationLat = locationLat;
    }

    public double getLocationLong() {
        return locationLong;
    }

    public void setLocationLong(double locationLong) {
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

    public double getRank() {
        return rank;
    }

    public void setRank(double rank) {
        this.rank = rank;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }
}