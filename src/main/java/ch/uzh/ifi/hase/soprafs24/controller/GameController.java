package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.mapper.DTOMapper;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class GameController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final GameService gameService;

    GameController(UserService userService, UserRepository userRepository, GameService gameService) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.gameService = gameService;
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
}
