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
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerLocationPutDTO;
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
import java.util.Optional;

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
                       UserService userService,
                       @Qualifier("playerRepository") PlayerRepository playerRepository) {
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

                    // --- Check if User already has an associated Player entity ---
                    Optional<Player> existingPlayerOpt = playerRepository.findByUser(user);

                    if (existingPlayerOpt.isPresent()) {
                        // --- User ALREADY HAS a Player entity ---
                        Player existingPlayer = existingPlayerOpt.get();

                        // --- CHECK: Is this player already in another active game? ---
                        // (Assumes Player.getGame() returns null if not in a game)
                        if (existingPlayer.getGame() != null && !existingPlayer.getGame().getGameId().equals(gameId)) {
                            // You might want to check the status of existingPlayer.getGame() too
                            // e.g., allow joining if the other game is FINISHED/CANCELLED
                            throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already participating in another game.");
                        }

                        // --- Associate EXISTING player with THIS game ---
                        existingPlayer.setLocationLat(gamePutDTO.getLocationLat());
                        existingPlayer.setLocationLong(gamePutDTO.getLocationLong());
                        existingPlayer.setGame(game); // Update game association
                        game.addPlayer(existingPlayer); // Add to this game's list

                        playerRepository.save(existingPlayer); // Save updated player

                    } else {
                        // --- User DOES NOT HAVE a Player entity yet (First game interaction is joining) ---
                        // Create a NEW Player entity for this user
                        Player newPlayer = new Player();
                        newPlayer.setLocationLat(gamePutDTO.getLocationLat());
                        newPlayer.setLocationLong(gamePutDTO.getLocationLong());
                        newPlayer.setUser(user);
                        newPlayer.setGame(game); // Associate with this game

                        game.addPlayer(newPlayer); // Add to this game's list

                        // Save the NEW player (this also links User <-> Player)
                        playerRepository.save(newPlayer);
                    }
                }
                else {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is full");
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

    public void updatePlayerLocation(Long gameId, User user, PlayerLocationPutDTO locationDTO) {
        Game game = gameRepository.findByGameId(gameId);
        if (game == null) {
            // Consider throwing custom GameNotFoundException
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found");
        }

        // Optional: Check if game is actually in progress
        if (game.getStatus() != GameStatus.IN_GAME /* && game.getStatus() != GameStatus.IN_GAME_PREPARATION */ ) {
            // Consider throwing custom GameNotActiveException
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Game is not active");
        }

        // Find the player associated with the authenticated user in this game
        Player playerToUpdate = game.getPlayers().stream()
                .filter(p -> p.getUser().getUserId().equals(user.getUserId()))
                .findFirst()
                // Consider throwing custom PlayerNotFoundInGameException
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Authenticated player not found in this game"));

        // Update player's location
        playerToUpdate.setLocationLat(locationDTO.getLocationLat());
        playerToUpdate.setLocationLong(locationDTO.getLocationLong());

        // Save the updated player (cascading might handle this via Game, but explicit save is fine)
        playerRepository.save(playerToUpdate);
        playerRepository.flush(); // Or save game if cascading configured
        log.debug("Updated location for Player ID {} in Game ID {}", playerToUpdate.getPlayerId(), gameId);
    }
}
