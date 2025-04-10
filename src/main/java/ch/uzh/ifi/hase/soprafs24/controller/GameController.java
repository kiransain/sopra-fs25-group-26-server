package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GameController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final GameService gameService;
    private final GameRepository gameRepository;

    GameController(UserService userService, UserRepository userRepository, GameService gameService, GameRepository gameRepository) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.gameService = gameService;
        this.gameRepository = gameRepository;
    }

    @PostMapping("/games")
    @ResponseStatus(HttpStatus.CREATED)
    @ResponseBody
    public GameGetDTO createGame(@RequestBody GamePostDTO gamePostDTO, @RequestHeader("Authorization") String authorizationHeader) {
        User creator = userService.authenticateUser(authorizationHeader);
        Player player = gameService.createPlayer(gamePostDTO.getLocationLat(), gamePostDTO.getLocationLong(), creator);
        Game game = gameService.createGame(gamePostDTO.getGamename(), player);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

    }

    @GetMapping("/games")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<GameGetDTO> getAllGames(@RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        List<Game> games = gameService.getJoinableGames();
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(games);
    }

    @PutMapping("/games/{gameId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO updateGame(@RequestBody GamePutDTO gamePutDTO, @PathVariable Long gameId, @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        Game game = gameService.updateGame(gameId, user, gamePutDTO);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @GetMapping("/games/{gameId}/players")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public List<PlayerGetDTO> getPlayers(@PathVariable Long gameId, @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        List<Player> players = gameService.getPlayers(gameId);
        return DTOMapper.INSTANCE.convertEntityToPlayerGetDTO(players);
    }

    @PutMapping("/games/{gameId}/location")
    @ResponseStatus(HttpStatus.NO_CONTENT) // Or OK if returning updated player/game
    @ResponseBody // Keep ResponseBody if potentially returning data later
    public void updatePlayerLocation(@PathVariable Long gameId,
                                     @RequestBody PlayerLocationPutDTO locationDTO,
                                     @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        gameService.updatePlayerLocation(gameId, user, locationDTO);
    }
    @PostMapping("/games/{gameId}/join")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO joinGame(@PathVariable Long gameId, 
                            @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(
            gameService.joinGame(gameId, user)
        );
    }

    @PostMapping("/games/{gameId}/leave")
    @ResponseStatus(HttpStatus.OK)
    public void leaveGame(@PathVariable Long gameId,
                        @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        gameService.leaveGame(gameId, user);
    }

    @PostMapping("/games/{gameId}/start")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO startGame(@PathVariable Long gameId,
                            @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(
            gameService.startGame(gameId, user)
        );
    }

    @GetMapping("/games/{gameId}/canStart")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public boolean canStartGame(@PathVariable Long gameId,
                            @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        return gameService.canStartGame(gameId, user);
    }
}
