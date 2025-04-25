package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.PlayerRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the Game REST controller.
 *
 * @see GameController
 */
@WebMvcTest(GameController.class)
public class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserService userService;

    @MockBean
    private GameRepository gameRepository; // Required for Controller context loading

    @MockBean
    private UserRepository userRepository; // Required for Controller context loading

    @MockBean
    private PlayerRepository playerRepository; // Required for Controller context loading

    private User testUser;
    private Player testPlayer;
    private Game testGame;
    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "valid-token";
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("creator");
        testUser.setToken(validToken);

        testPlayer = new Player();
        testPlayer.setPlayerId(10L);
        testPlayer.setUser(testUser);
        testPlayer.setLocationLat(47.3769);
        testPlayer.setLocationLong(8.5417);
        testPlayer.setDisplayName(testUser.getUsername());
        testPlayer.setRole(PlayerRole.HIDER);
        testPlayer.setStatus(PlayerStatus.HIDING);

        testGame = new Game();
        testGame.setGameId(100L);
        testGame.setGamename("Test Game");
        testGame.setStatus(GameStatus.IN_LOBBY);
        testGame.setCreator(testPlayer);
        testGame.setCenterLatitude(47.3769);
        testGame.setCenterLongitude(8.5417);
        testGame.setRadius(100.0);
        testGame.setTimer(LocalDateTime.now().minusMinutes(5));
        testGame.setPlayers(new ArrayList<>(List.of(testPlayer))); // Use mutable list
        testPlayer.setGame(testGame);

        // Default authentication mock
        given(userService.authenticateUser(validToken)).willReturn(testUser);
    }

    @Test
    public void createGame_validInput_gameCreated() throws Exception {
        // given
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGamename("New Game");
        gamePostDTO.setLocationLat(47.0);
        gamePostDTO.setLocationLong(8.0);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.createPlayer(anyDouble(), anyDouble(), eq(testUser))).willReturn(testPlayer);
        given(gameService.createGame(eq("New Game"), eq(testPlayer))).willReturn(testGame);

        // when
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId", is(testGame.getGameId().intValue())))
                .andExpect(jsonPath("$.gamename", is(testGame.getGamename())))
                .andExpect(jsonPath("$.status", is(testGame.getStatus().toString())))
                .andExpect(jsonPath("$.creatorId", is(testPlayer.getPlayerId().intValue())))
                .andExpect(jsonPath("$.players", hasSize(1)))
                .andExpect(jsonPath("$.players[0].playerId", is(testPlayer.getPlayerId().intValue())))
                .andExpect(jsonPath("$.players[0].userId", is(testUser.getUserId().intValue())));
    }

    @Test
    public void createGame_gamenameConflict_throwsConflict() throws Exception {
        // given
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGamename("Existing Game");
        gamePostDTO.setLocationLat(47.0);
        gamePostDTO.setLocationLong(8.0);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.createPlayer(anyDouble(), anyDouble(), eq(testUser))).willReturn(testPlayer);
        given(gameService.createGame(eq("Existing Game"), eq(testPlayer)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Gamename already exists"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void createGame_userAlreadyPlayer_throwsConflict() throws Exception {
        // given
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGamename("Another Game");
        gamePostDTO.setLocationLat(47.0);
        gamePostDTO.setLocationLong(8.0);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.createPlayer(anyDouble(), anyDouble(), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "User is already a Player"));

        // when
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePostDTO));

        // then
        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void getAllGames_success_returnsJoinableGames() throws Exception {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        List<Game> joinableGames = Collections.singletonList(testGame);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.getJoinableGames()).willReturn(joinableGames);

        // when
        MockHttpServletRequestBuilder getRequest = get("/games")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].gameId", is(testGame.getGameId().intValue())))
                .andExpect(jsonPath("$[0].gamename", is(testGame.getGamename())))
                .andExpect(jsonPath("$[0].status", is(GameStatus.IN_LOBBY.toString())));
    }

    @Test
    public void getAllGames_invalidToken_throwsUnauthorized() throws Exception {
        // given
        String invalidToken = "invalid-token";
        given(userService.authenticateUser(invalidToken))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization failed"));

        // when
        MockHttpServletRequestBuilder getRequest = get("/games")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateGame_joinGame_success() throws Exception {
        // given
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setLocationLat(47.1);
        gamePutDTO.setLocationLong(8.1);
        gamePutDTO.setStartGame(false);

        long gameId = testGame.getGameId();
        User joiningUser = new User();
        joiningUser.setUserId(2L);
        joiningUser.setUsername("joiner");
        joiningUser.setToken("joiner-token");

        given(userService.authenticateUser("joiner-token")).willReturn(joiningUser);
        given(gameService.updateGame(eq(gameId), eq(joiningUser), any(GamePutDTO.class)))
                .willReturn(testGame); // Assume service handles adding player

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}", gameId)
                .header("Authorization", "joiner-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePutDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(testGame.getGameId().intValue())));
    }


    @Test
    public void updateGame_startGameByCreator_success() throws Exception {
        // given
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setLocationLat(testPlayer.getLocationLat());
        gamePutDTO.setLocationLong(testPlayer.getLocationLong());
        gamePutDTO.setStartGame(true);

        long gameId = testGame.getGameId();
        testGame.setStatus(GameStatus.IN_LOBBY);

        // Add enough players
        User user2 = new User(); user2.setUserId(2L); user2.setUsername("player2");
        Player player2 = new Player(); player2.setPlayerId(11L); player2.setUser(user2);
        User user3 = new User(); user3.setUserId(3L); user3.setUsername("player3");
        Player player3 = new Player(); player3.setPlayerId(12L); player3.setUser(user3);
        testGame.addPlayer(player2);
        testGame.addPlayer(player3);

        Game startedGame = testGame;
        startedGame.setStatus(GameStatus.IN_GAME_PREPARATION); // Simulate state change

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.updateGame(eq(gameId), eq(testUser), any(GamePutDTO.class)))
                .willReturn(startedGame);

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}", gameId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePutDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(startedGame.getGameId().intValue())))
                .andExpect(jsonPath("$.status", is(GameStatus.IN_GAME_PREPARATION.toString())));
    }

    @Test
    public void updateGame_startGameNotCreator_throwsForbidden() throws Exception {
        // given
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setLocationLat(47.2);
        gamePutDTO.setLocationLong(8.2);
        gamePutDTO.setStartGame(true);

        long gameId = testGame.getGameId();
        User notCreator = new User();
        notCreator.setUserId(99L);
        notCreator.setUsername("notCreator");
        notCreator.setToken("not-creator-token");

        given(userService.authenticateUser("not-creator-token")).willReturn(notCreator);
        given(gameService.updateGame(eq(gameId), eq(notCreator), any(GamePutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the creator can start the game"));

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}", gameId)
                .header("Authorization", "not-creator-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePutDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateGame_gameNotFound_throwsNotFound() throws Exception {
        // given
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setLocationLat(47.0);
        gamePutDTO.setLocationLong(8.0);
        gamePutDTO.setStartGame(false);
        long nonExistentGameId = 999L;

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.updateGame(eq(nonExistentGameId), eq(testUser), any(GamePutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}", nonExistentGameId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(gamePutDTO));

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }


    @Test
    public void updatePlayer_admitCaught_success() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long playerId = testPlayer.getPlayerId();
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayer.setStatus(PlayerStatus.HIDING);

        Game updatedGame = testGame;
        updatedGame.getPlayers().get(0).setStatus(PlayerStatus.FOUND); // Simulate update

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.updatePlayer(eq(gameId), eq(playerId), eq(testUser)))
                .willReturn(updatedGame);

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(updatedGame.getGameId().intValue())))
                .andExpect(jsonPath("$.players[?(@.playerId == " + playerId + ")].status", contains(PlayerStatus.FOUND.toString())));
    }

    @Test
    public void updatePlayer_playerNotFound_throwsNotFound() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long nonExistentPlayerId = 999L;

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.updatePlayer(eq(gameId), eq(nonExistentPlayerId), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}/players/{playerId}", gameId, nonExistentPlayerId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void updatePlayer_forbidden_throwsForbidden() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long playerId = testPlayer.getPlayerId();
        testGame.setStatus(GameStatus.FINISHED); // Example: Game not IN_GAME

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        given(gameService.updatePlayer(eq(gameId), eq(playerId), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Player is not in that game or user is not that player"));

        // when
        MockHttpServletRequestBuilder putRequest = put("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }


    @Test
    public void deletePlayer_leaveLobbyNotCreator_success() throws Exception {
        // given
        long gameId = testGame.getGameId();
        testGame.setStatus(GameStatus.IN_LOBBY);

        User leavingUser = new User(); leavingUser.setUserId(2L); leavingUser.setUsername("leaver"); leavingUser.setToken("leaver-token");
        Player leavingPlayer = new Player(); leavingPlayer.setPlayerId(11L); leavingPlayer.setUser(leavingUser);
        testGame.addPlayer(leavingPlayer);
        long leavingPlayerId = leavingPlayer.getPlayerId();

        given(userService.authenticateUser("leaver-token")).willReturn(leavingUser);
        doNothing().when(gameService).deletePlayer(eq(gameId), eq(leavingPlayerId), eq(leavingUser));

        // when
        MockHttpServletRequestBuilder deleteRequest = delete("/games/{gameId}/players/{playerId}", gameId, leavingPlayerId)
                .header("Authorization", "leaver-token");

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void deletePlayer_leaveLobbyCreator_successDeletesGame() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long creatorPlayerId = testPlayer.getPlayerId();
        testGame.setStatus(GameStatus.IN_LOBBY);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        doNothing().when(gameService).deletePlayer(eq(gameId), eq(creatorPlayerId), eq(testUser));

        // when
        MockHttpServletRequestBuilder deleteRequest = delete("/games/{gameId}/players/{playerId}", gameId, creatorPlayerId)
                .header("Authorization", validToken);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNoContent());
    }

    @Test
    public void deletePlayer_gameNotInLobby_throwsConflict() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long playerId = testPlayer.getPlayerId();
        testGame.setStatus(GameStatus.IN_GAME);

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started, cannot leave game"))
                .when(gameService).deletePlayer(eq(gameId), eq(playerId), eq(testUser));

        // when
        MockHttpServletRequestBuilder deleteRequest = delete("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", validToken);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void deletePlayer_notThePlayer_throwsForbidden() throws Exception {
        // given
        long gameId = testGame.getGameId();
        long playerId = testPlayer.getPlayerId();
        testGame.setStatus(GameStatus.IN_LOBBY);

        User maliciousUser = new User(); maliciousUser.setUserId(99L); maliciousUser.setUsername("hacker"); maliciousUser.setToken("hacker-token");

        given(userService.authenticateUser("hacker-token")).willReturn(maliciousUser);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "User is not that player"))
                .when(gameService).deletePlayer(eq(gameId), eq(playerId), eq(maliciousUser));

        // when
        MockHttpServletRequestBuilder deleteRequest = delete("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", "hacker-token");

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isForbidden());
    }

    @Test
    public void deletePlayer_gameNotFound_throwsNotFound() throws Exception {
        // given
        long nonExistentGameId = 999L;
        long playerId = testPlayer.getPlayerId();

        given(userService.authenticateUser(validToken)).willReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"))
                .when(gameService).deletePlayer(eq(nonExistentGameId), eq(playerId), eq(testUser));

        // when
        MockHttpServletRequestBuilder deleteRequest = delete("/games/{gameId}/players/{playerId}", nonExistentGameId, playerId)
                .header("Authorization", validToken);

        // then
        mockMvc.perform(deleteRequest)
                .andExpect(status().isNotFound());
    }


    /**
     * Helper Method to convert object into JSON string.
     * @param object the object to convert
     * @return JSON string
     */
    private String asJsonString(final Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules(); // Register JavaTimeModule etc.
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
