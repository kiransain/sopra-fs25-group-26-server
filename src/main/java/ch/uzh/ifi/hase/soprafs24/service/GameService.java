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
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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

    public Player createPlayer(double locationLatitude, double locationLongitude, User user) {
        // Check if user is already a player
        if (playerRepository.findByUser(user).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a Player");
        }

        Player player = new Player();
        player.setDisplayName(user.getUsername());
        player.setLocationLat(locationLatitude);
        player.setLocationLong(locationLongitude);
        player.setUser(user);
        playerRepository.save(player);
        playerRepository.flush();
        return player;
    }

    public Game createGame(String gamename, Player player) {
        List<Game> activeGames = gameRepository.findByStatusNot(GameStatus.FINISHED);

        boolean nameTaken = activeGames.stream()
                .anyMatch(game -> game.getGamename().equals(gamename));

        if (nameTaken) {
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
                Player player = createPlayer(gamePutDTO.getLocationLat(), gamePutDTO.getLocationLong(), user);
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
            if (game.getCreator().getUser().getUserId().equals(user.getUserId()) && game.getPlayers().size() > 1) {
                game.setStatus(GameStatus.IN_GAME_PREPARATION);
                game = start(game);
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can start the game or not enough players");
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

    public Game updatePlayer(Long gameId, long playerId, User user) {
        //find game
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        //find player
        Player player = playerRepository.findPlayerByPlayerId(playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }

        //check if player is in that game and user it that player and Game is in Game
        if (game.getPlayers().contains(player) && player.getUser().getUserId().equals(user.getUserId()) && game.getStatus() == GameStatus.IN_GAME) {
            //check if player is hider and not found yet
            if (player.getRole() == PlayerRole.HIDER && player.getStatus() == PlayerStatus.HIDING) {
                player.setStatus(PlayerStatus.FOUND);
                player.setFoundTime(LocalDateTime.now());
                game.setRadius(game.getRadius() - 25);
                game = checkEndCondition(game);
            }
            else {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not a hider or already found");
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not in that game or user is not that player");
        }
        gameRepository.save(game);
        gameRepository.flush();
        return game;
    }

    public Game checkEndCondition(Game game) {
        // check if all hiders are found
        boolean allHidersFound = game.getPlayers().stream()
                .filter(player -> player.getRole() == PlayerRole.HIDER)
                .allMatch(player -> player.getStatus() == PlayerStatus.FOUND);

        if (allHidersFound) {
            game.setStatus(GameStatus.FINISHED);
            gameTimerService.stopFinishTimer(game.getGameId());
            log.info("Game {} finished", game.getGameId());
            game = computeRankings(game);
            gameRepository.save(game);
            gameRepository.flush();


        }
        return game;
    }


    public Game computeRankings(Game game) {
        // Get the list of hiders sorted with null foundTime first
        System.out.println("Computing rankings for game: " + game.getGameId());
        List<Player> hiders = game.getPlayers().stream()
                .filter(player -> player.getRole() == PlayerRole.HIDER)
                .sorted(Comparator.comparing(Player::getFoundTime, Comparator.nullsFirst(Comparator.naturalOrder())))
                .collect(Collectors.toList());
        System.out.println("After Hiders List");

        // For hiders who have not been caught (null foundTime), assign rank 1.
        // For hiders who have a foundTime (were caught), start ranking from 2.
        int nextRank = 2;
        for (Player hider : hiders) {
            if (hider.getFoundTime() == null) {
                hider.setRank(1);
            }
            else {
                hider.setRank(nextRank);
                nextRank++;
            }
        }

        // Assign rank to the hunter
        // If all hiders are caught (none have a null foundTime), then the hunter wins and should be rank 1.
        // Otherwise, if at least one hider is not caught, the hunter gets lowest rank
        Player hunter = game.getPlayers().stream()
                .filter(player -> player.getRole() == PlayerRole.HUNTER)
                .findFirst().orElse(null);

        // Check if all hiders have been caught (none with null foundTime)
        boolean allHidersCaught = hiders.stream().noneMatch(hider -> hider.getFoundTime() == null);

        if (hunter != null) {
            if (allHidersCaught) {
                // If all hiders are caught, assign hunter rank 1
                hunter.setRank(1);
            }
            else {
                // If any hider is not caught, let the hunter get the worst rank among the participants.
                double lastRankHiders = hiders.get(hiders.size() - 1).getRank();
                hunter.setRank(lastRankHiders + 1);
            }
        }
        System.out.println("Setting stats for game: " + game.getGameId());

        updateStats(game);

        return game;

    }

    public void updateStats(Game game) {
        try {
            System.out.println("Updating stats for game: " + game.getGameId());
            Long gameId = game.getGameId();
            Game refreshedGame = gameRepository.findByGameId(gameId);

            for (Player player : refreshedGame.getPlayers()) {
                User user = player.getUser();
                if (player.getRank() == 1) {
                    String winsStr = user.getStats().getOrDefault("wins", "0");
                    String gamesPlayedStr = user.getStats().getOrDefault("gamesPlayed", "0");
                    int wins = Integer.parseInt(winsStr);
                    int gamesPlayed = Integer.parseInt(gamesPlayedStr);
                    wins++;
                    gamesPlayed++;
                    user.getStats().put("wins", Integer.toString(wins));
                    user.getStats().put("gamesPlayed", Integer.toString(gamesPlayed));
                    System.out.println("Setting stats for user: " + user.getUserId());
                    userRepository.save(user);
                    userRepository.flush();
                    System.out.println("Stats set for user: " + user.getUserId());
                }
                else {
                    String gamesPlayedStr = user.getStats().getOrDefault("gamesPlayed", "0");
                    int gamesPlayed = Integer.parseInt(gamesPlayedStr);
                    gamesPlayed++;
                    user.getStats().put("gamesPlayed", Integer.toString(gamesPlayed));
                    System.out.println("Setting stats for user: " + user.getUserId());
                    userRepository.save(user);
                    userRepository.flush();
                    System.out.println("Stats set for user: " + user.getUserId());
                }
            }
            // decoupling user from player so that user can join/create a new game
            for (Player player : game.getPlayers()) {
                player.setUser(null);
            }
            System.out.println("Finished updating stats for game: " + game.getGameId());
        }
        catch (Exception e) {
            log.error("Error updating stats for game: " + game.getGameId(), e);

        }
    }

    public void finishGame(Long gameId) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        game.setStatus(GameStatus.FINISHED);
        game = computeRankings(game);
        gameRepository.save(game);
        gameRepository.flush();

    }

    public void deletePlayer(Long gameId, long playerId, User user) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }
        if (game.getStatus() != GameStatus.IN_LOBBY) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started, cannot leave game");
        }
        Player player = playerRepository.findPlayerByPlayerId(playerId);
        if (player == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
        }
        // check if player is in that game and user is that player
        if (game.getPlayers().contains(player) && player.getUser().getUserId().equals(user.getUserId())) {
            //player is creator
            if (game.getCreator().equals(player)) {
                gameRepository.delete(game);
            }
            //player is not creator
            else {
                game.getPlayers().remove(player);
                gameRepository.save(game);
                gameRepository.flush();
            }
        }
        else {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not in that game or User is not that player");
        }
    }
}
