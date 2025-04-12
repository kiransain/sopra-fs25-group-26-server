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

import java.time.LocalDateTime;
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
    private GameTimerService gameTimerService;

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
            game = handleInLobby(game, user, gamePutDTO);
        }

        if (game.getStatus() == GameStatus.IN_GAME_PREPARATION) {
            game = handleInGamePreparation(game, user, gamePutDTO);
        }

        if (game.getStatus() == GameStatus.IN_GAME) {
            game = handleInGame(game, user, gamePutDTO);
        }

        return game;
    }

    public Game handleInGame(Game game, User user, GamePutDTO gamePutDTO) {
        boolean alreadyJoined = game.getPlayers().stream()
                .anyMatch(player -> player.getUser().getUserId().equals(user.getUserId()));
        // update location if user is already in game
        if (alreadyJoined) {
            game.getPlayers().stream()
                    .filter(player -> player.getUser().getUserId().equals(user.getUserId()))
                    .forEach(player -> {
                        player.setLocationLat(gamePutDTO.getLocationLat());
                        player.setLocationLong(gamePutDTO.getLocationLong());
                        // calculate distance of player to center of game area
                        double distance = calculateDistance(
                                player.getLocationLat(), player.getLocationLong(),
                                game.getCenterLatitude(), game.getCenterLongitude());
                        player.setOutOfArea(distance > game.getRadius());
                    });

        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game has already started");
        }

        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public double calculateDistance(double lat1, double long1, double lat2, double long2) {
        final int EARTH_RADIUS = 6371; // Earth radius in kilometers

        // Convert degrees to radians
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(long2 - long1);

        // Haversine formula:
        // a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);

        // c = 2 * atan2(√a, √(1–a))
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        // Distance = Earth's radius * c * 1000 - 5 (tolerance because of GPS)
        return (EARTH_RADIUS * c * 1000) - 5;
    }

    public Game handleInGamePreparation(Game game, User user, GamePutDTO gamePutDTO) {
        boolean alreadyJoined = game.getPlayers().stream()
                .anyMatch(player -> player.getUser().getUserId().equals(user.getUserId()));
        // update location if user is already in game
        if (alreadyJoined) {
            game.getPlayers().stream()
                    .filter(player -> player.getUser().getUserId().equals(user.getUserId()))
                    .forEach(player -> {
                        player.setLocationLat(gamePutDTO.getLocationLat());
                        player.setLocationLong(gamePutDTO.getLocationLong());
                    });
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game has already started");
        }

        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public Game handleInLobby(Game game, User user, GamePutDTO gamePutDTO) {
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
                game = start(game);
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can start the game");
            }
        }
        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public Game start(Game game) {
        List<Player> players = game.getPlayers();
        Collections.shuffle(players);
        // assign roles
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
        // set radius based on player count
        double radius = 25 * players.size();
        game.setRadius(radius);
        // set a timestamp for timer so that client can use that to display timer
        game.setTimer(LocalDateTime.now());
        //start scheduler to change status after timer runs out
        gameTimerService.startPreparationTimer(game.getGameId());

        gameRepository.save(game);
        gameRepository.flush();
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
