package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameCenterDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePutDTO;
import ch.uzh.ifi.hase.soprafs24.service.GameService;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GameController.class)
public class GameControllerTest {

    private final String VALID_TOKEN = "Bearer valid-token";
    private final String INVALID_TOKEN = "Bearer invalid-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private GameRepository gameRepository;

    private User testUser;
    private Player testPlayer;
    private Game testGame;
    private GamePostDTO testGamePostDTO;
    private GamePutDTO testGamePutDTO;

    @BeforeEach
    public void setup() {
        // User setup
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("Tester");
        testUser.setPassword("Test_Password");
        testUser.setToken("valid-token");
        testUser.setProfilePicture("https://example.com/profile.jpg");
        testUser.setStats(new HashMap<>());

        // Player setup
        testPlayer = new Player();
        testPlayer.setPlayerId(10L);
        testPlayer.setUser(testUser);
        testPlayer.setDisplayName(testUser.getUsername());
        testPlayer.setDisplayPicture(testUser.getProfilePicture());
        testPlayer.setLocationLat(47.0);
        testPlayer.setLocationLong(8.0);


        // Game setup
        testGame = new Game();
        testGame.setGameId(100L);
        testGame.setGamename("Test Game");
        testGame.setStatus(GameStatus.IN_LOBBY);
        testGame.setCreator(testPlayer);
        testGame.setCenterLatitude(47.0);
        testGame.setCenterLongitude(8.0);
        testGame.setRadius(100.0);
        testGame.setPreparationTimeInSeconds(30);
        testGame.setGameTimeInSeconds(300);
        testGame.setPlayers(new ArrayList<>(List.of(testPlayer)));
        testPlayer.setGame(testGame);

        // DTO setup
        testGamePostDTO = new GamePostDTO();
        testGamePostDTO.setGamename("Test Game");
        testGamePostDTO.setLocationLat(47.0);
        testGamePostDTO.setLocationLong(8.0);
        testGamePostDTO.setRadius(100.0);
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(300);

        testGamePutDTO = new GamePutDTO();
        testGamePutDTO.setLocationLat(47.1);
        testGamePutDTO.setLocationLong(8.1);
        testGamePutDTO.setStartGame(false);
    }

    /**
     * Converts Object to JSON String
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }

    // GET /games - Get all joinable games
    @Test
    public void givenGames_whenGetGames_thenReturnJsonArray() throws Exception {
        // given
        List<Game> allJoinableGames = Collections.singletonList(testGame);
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.getJoinableGames()).willReturn(allJoinableGames);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/games")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].gameId", is(testGame.getGameId().intValue())))
                .andExpect(jsonPath("$[0].gamename", is(testGame.getGamename())))
                .andExpect(jsonPath("$[0].status", is(testGame.getStatus().toString())))
                .andExpect(jsonPath("$[0].players", hasSize(1)));
    }

    @Test
    public void givenInvalidToken_whenGetGames_thenReturn401() throws Exception {
        // given
        given(userService.authenticateUser(INVALID_TOKEN))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(get("/games")
                        .header("Authorization", INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // POST /games - Create a new game
    @Test
    public void createGame_validInput_success() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.createGame(any(GamePostDTO.class), eq(testUser))).willReturn(testGame);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameId", is(testGame.getGameId().intValue())))
                .andExpect(jsonPath("$.gamename", is(testGame.getGamename())))
                .andExpect(jsonPath("$.status", is(testGame.getStatus().toString())));
    }

    @Test
    public void createGame_duplicateGamename_throwsConflict() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.createGame(any(GamePostDTO.class), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Gamename already exists"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void createGame_invalidParameters_throwsBadRequest() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.createGame(any(GamePostDTO.class), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parameters"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/games")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isBadRequest());
    }

    // PUT /games/{gameId} - Update a game (join, update location, start)
    @Test
    public void updateGame_joinLobby_success() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGame(eq(testGame.getGameId()), eq(testUser), any(GamePutDTO.class)))
                .willReturn(testGame);

        // when/then
        MockHttpServletRequestBuilder putRequest = put("/games/" + testGame.getGameId())
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(testGame.getGameId().intValue())))
                .andExpect(jsonPath("$.gamename", is(testGame.getGamename())));
    }

    @Test
    public void updateGame_startGame_success() throws Exception {
        // given
        testGame.setStatus(GameStatus.IN_GAME_PREPARATION);
        testGamePutDTO.setStartGame(true);
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGame(eq(testGame.getGameId()), eq(testUser), any(GamePutDTO.class)))
                .willReturn(testGame);

        // when/then
        MockHttpServletRequestBuilder putRequest = put("/games/" + testGame.getGameId())
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(testGame.getStatus().toString())));
    }

    @Test
    public void updateGame_gameNotFound_returnsNotFound() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGame(eq(999L), eq(testUser), any(GamePutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // when/then
        MockHttpServletRequestBuilder putRequest = put("/games/999")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void updateGame_gameFull_returnsForbidden() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGame(eq(testGame.getGameId()), eq(testUser), any(GamePutDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Game is full"));

        // when/then
        MockHttpServletRequestBuilder putRequest = put("/games/" + testGame.getGameId())
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(testGamePutDTO));

        mockMvc.perform(putRequest)
                .andExpect(status().isForbidden());
    }

    // PUT /games/{gameId}/players/{playerId} - Update a player (admit caught)
    @Test
    public void updatePlayer_admitCaught_success() throws Exception {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updatePlayer(testGame.getGameId(), testPlayer.getPlayerId(), testUser))
                .willReturn(testGame);

        // when/then
        mockMvc.perform(put("/games/" + testGame.getGameId() + "/players/" + testPlayer.getPlayerId())
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameId", is(testGame.getGameId().intValue())));
    }

    @Test
    public void updatePlayer_notFound_returnsNotFound() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updatePlayer(testGame.getGameId(), 999L, testUser))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found"));

        // when/then
        mockMvc.perform(put("/games/" + testGame.getGameId() + "/players/999")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    public void updatePlayer_notUsersPLayer_returnsForbidden() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updatePlayer(testGame.getGameId(), testPlayer.getPlayerId(), testUser))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not user's player"));

        // when/then
        mockMvc.perform(put("/games/" + testGame.getGameId() + "/players/" + testPlayer.getPlayerId())
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isForbidden());
    }

    // DELETE /games/{gameId}/players/{playerId} - Delete a player (leave game)
    @Test
    public void deletePlayer_leaveLobby_success() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        doNothing().when(gameService).deletePlayer(testGame.getGameId(), testPlayer.getPlayerId(), testUser);

        // when/then
        mockMvc.perform(delete("/games/" + testGame.getGameId() + "/players/" + testPlayer.getPlayerId())
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isNoContent());

        verify(gameService, times(1)).deletePlayer(testGame.getGameId(), testPlayer.getPlayerId(), testUser);
    }

    @Test
    public void deletePlayer_gameNotFound_returnsNotFound() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"))
                .when(gameService).deletePlayer(999L, testPlayer.getPlayerId(), testUser);

        // when/then
        mockMvc.perform(delete("/games/999/players/" + testPlayer.getPlayerId())
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    @Test
    public void deletePlayer_alreadyStarted_returnsConflict() throws Exception {
        // given
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Game has already started"))
                .when(gameService).deletePlayer(testGame.getGameId(), testPlayer.getPlayerId(), testUser);

        // when/then
        mockMvc.perform(delete("/games/" + testGame.getGameId() + "/players/" + testPlayer.getPlayerId())
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isConflict());
    }

    // PUT /games/{gameId}/center - Update game center
    @Test
    public void updateGameCenter_success() throws Exception {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testGame.setCenterLatitude(47.1);
        testGame.setCenterLongitude(8.1);

        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.1);
        centerDTO.setLongitude(8.1);


        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGameCenter(eq(testGame.getGameId()), any(GameCenterDTO.class), eq(testUser)))
                .willReturn(testGame);


        // when/then
        MockHttpServletRequestBuilder putRequest = put("/games/" + testGame.getGameId() + "/center")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(centerDTO));

        mockMvc.perform(putRequest)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath(("$.gameId"), is(testGame.getGameId().intValue())))
                .andExpect(jsonPath(("$.centerLatitude"), is(testGame.getCenterLatitude())))
                .andExpect(jsonPath("$.centerLongitude", is(testGame.getCenterLongitude())));
    }

    @Test
    public void updateGameCenter_tooFarAway_returnsForbidden() throws Exception {
        // given
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(48.0);
        centerDTO.setLongitude(9.0);

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGameCenter(eq(testGame.getGameId()), any(GameCenterDTO.class), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "New center is out of current game area"));

        // when/then
        mockMvc.perform(put("/games/" + testGame.getGameId() + "/center")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(centerDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateGameCenter_notHunter_returnsForbidden() throws Exception {
        // given
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.1);
        centerDTO.setLongitude(8.1);

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGameCenter(eq(testGame.getGameId()), any(GameCenterDTO.class), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the hunter can update the game center"));

        // when/then
        mockMvc.perform(put("/games/" + testGame.getGameId() + "/center")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(centerDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateGameCenter_unauthorized_returns401() throws Exception {
        // given
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.1);
        centerDTO.setLongitude(8.1);

        given(userService.authenticateUser("Bearer invalid-token"))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(put("/games/100/center")
                        .header("Authorization", "Bearer invalid-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(centerDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateGameCenter_gameNotFound_returns404() throws Exception {
        // given
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.1);
        centerDTO.setLongitude(8.1);

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(testUser);
        given(gameService.updateGameCenter(any(Long.class), any(GameCenterDTO.class), eq(testUser)))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Game not found"));

        // when/then
        mockMvc.perform(put("/games/999/center")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(centerDTO)))
                .andExpect(status().isNotFound());
    }
}