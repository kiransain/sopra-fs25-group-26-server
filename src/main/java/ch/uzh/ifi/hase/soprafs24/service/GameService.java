package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;

@Service
@Transactional
public class GameService {
    private final Logger log = LoggerFactory.getLogger(GameService.class);

    private final GameRepository gameRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final PlayerRepository playerRepository;

    @Autowired
    public GameService(@Qualifier("gameRepository") GameRepository gameRepository,
                       @Qualifier("userRepository") UserRepository userRepository,
                       UserService userService, PlayerRepository playerRepository) {
        this.gameRepository = gameRepository;
        this.userRepository = userRepository;
        this.userService = userService;
        this.playerRepository = playerRepository;
    }

    public List<Game> getGames() {
        return gameRepository.findAll();
    }

    public Player createPlayer(double locationLatitude, double locationLongitude, User creator) {
        // Check if user is already a player
        if (playerRepository.findByUser(creator).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a Player");
        }

        Player player = new Player();
        player.setLocationLat(locationLatitude);
        player.setLocationLong(locationLongitude);
        player.setUser(creator);
        playerRepository.save(player);
        playerRepository.flush();
        return player;
    }

    public Game createGame(String gamename, Player player) {
        if (gameRepository.findByGamename(gamename) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Gamename already exists");
        }
        Game game = new Game();
        game.setGamename(gamename);
        game.addPlayer(player);
        game.setStatus(GameStatus.IN_LOBBY);
        game.setCreator(player);
        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public List<Game> getJoinableGames() {
        List<Game> games = gameRepository.findByStatus(GameStatus.IN_LOBBY);
        return games;
    }

    public Game updateGame(Long gameId, User user, GamePutDTO gamePutDTO) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        if (game.getStatus() == GameStatus.IN_LOBBY) {
            boolean alreadyJoined = game.getPlayers().stream()
                    .anyMatch(player -> player.getUser().getUserId().equals(user.getUserId()));
            // if player not already in game, add him
            if (!alreadyJoined) {
                if (game.getPlayers().size() < 5) {
                    Player player = new Player();
                    player.setLocationLat(gamePutDTO.getLocationLat());
                    player.setLocationLong(gamePutDTO.getLocationLong());
                    player.setUser(user);
                    game.addPlayer(player);
                }
                else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is full");
                }
            }
            // if player already in game, update location
            else {
                game.getPlayers().stream()
                        .filter(player -> player.getUser().getUserId().equals(user.getUserId()))
                        .forEach(player -> {
                            player.setLocationLat(gamePutDTO.getLocationLat());
                            player.setLocationLong(gamePutDTO.getLocationLong());
                        });
            }
            // if a player wants to start game, check if it is creator
            if (gamePutDTO.isStartGame()) {
                if (game.getCreator().getUser().getUserId().equals(user.getUserId()) && game.getPlayers().size() > 2) {
                    game.setStatus(GameStatus.IN_GAME_PREPARATION);
                    game = assignRoles(game);
                }
                else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can start the game");
                }
            }
        }
        // implement logic for when game has started
        //if (game.getStatus() == GameStatus.IN_GAME) {
        //boolean alreadyJoined = game.getPlayers().stream()
        //.anyMatch(player -> player.getUser().getUserId().equals(user.getUserId()));
        //}
        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public Game assignRoles(Game game) {
        List<Player> players = game.getPlayers();
        Collections.shuffle(players);

        for (int i = 0; i < players.size(); i++) {
            Player player = players.get(i);
            if (i == 0) {
                player.setRole(PlayerRole.HUNTER);
                player.setStatus(PlayerStatus.HUNTING);
                game.setCenterLatitude(player.getLocationLat());
                game.setCenterLongitude(player.getLocationLong());
            }
            else {
                player.setRole(PlayerRole.HIDER);
                player.setStatus(PlayerStatus.HIDING);
            }
        }
        return game;
    }

    public List<Player> getPlayers(Long gameId) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        return game.getPlayers();

    }
}
