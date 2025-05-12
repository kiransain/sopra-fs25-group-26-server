package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameCenterDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
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
        Game game = gameService.createGame(gamePostDTO, creator);
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

    @PutMapping("/games/{gameId}/center")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO updateGameCenter(@RequestBody GameCenterDTO gameCenterDTO, @PathVariable Long gameId, @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        Game game = gameService.updateGameCenter(gameId, gameCenterDTO, user);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @PutMapping("games/{gameId}/players/{playerId}")
    @ResponseStatus(HttpStatus.OK)
    @ResponseBody
    public GameGetDTO updatePlayer(@PathVariable Long gameId, @PathVariable Long playerId, @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        Game game = gameService.updatePlayer(gameId, playerId, user);
        return DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);
    }

    @DeleteMapping("/games/{gameId}/players/{playerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @ResponseBody
    public void deletePlayer(@PathVariable Long gameId, @PathVariable Long playerId, @RequestHeader("Authorization") String authorizationHeader) {
        User user = userService.authenticateUser(authorizationHeader);
        gameService.deletePlayer(gameId, playerId, user);
    }
}

