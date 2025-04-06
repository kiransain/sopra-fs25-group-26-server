package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
}
