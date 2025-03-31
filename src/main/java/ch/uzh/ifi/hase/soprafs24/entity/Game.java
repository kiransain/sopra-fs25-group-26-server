package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "GAME")
public class Game implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue
    private Long gameId;

    @Column(nullable = false, unique = true)
    private String gameName;

    @Column(nullable = false)
    private String creationDate;

    @Column()
    private double centerLatitude;

    @Column()
    private double centerLongitude;

    @Column()
    private double radius;

    @Column()
    private GameStatus status;

    //tells JPA that one game can have multiple player where the game field in players is the link,
    //deleting a game will delete all players and if player removed from game, it will also be removed from DB
    @OneToMany(mappedBy = "game", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Player> players = new ArrayList<>();
    ;


}
