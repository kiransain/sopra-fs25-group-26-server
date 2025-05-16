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
import ch.uzh.ifi.hase.soprafs24.rest.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for game-related functionalities, focusing on interactions
 * between Controller, Service, Repository, and Database layers.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional // Roll back database changes after each test
public class GameIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private GameRepository gameRepository;

    @Autowired
    private PlayerRepository playerRepository;

    // Helper to create a user via API and return its DTO
    private UserGetDTO performCreateUser(String username, String password) throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(username);
        userPostDTO.setPassword(password);

        MvcResult result = mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPostDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserGetDTO.class);
    }

    // Helper to login a user via API and return its DTO (including token)
    private UserGetDTO performLoginUser(String username, String password) throws Exception {
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername(username);
        userPostDTO.setPassword(password);

        MvcResult result = mockMvc.perform(post("/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userPostDTO)))
                .andExpect(status().isAccepted())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), UserGetDTO.class);
    }

    // Helper to create a game via API and return its DTO
    private GameGetDTO performCreateGame(String token, String gameName, double lat, double lon, double radius, Integer prepTime, Integer gameTime) throws Exception {
        GamePostDTO gamePostDTO = new GamePostDTO();
        gamePostDTO.setGamename(gameName);
        gamePostDTO.setLocationLat(lat);
        gamePostDTO.setLocationLong(lon);
        gamePostDTO.setRadius(radius);
        gamePostDTO.setPreparationTimeInSeconds(prepTime);
        gamePostDTO.setGameTimeInSeconds(gameTime);

        MvcResult result = mockMvc.perform(post("/games")
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gamePostDTO)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(result.getResponse().getContentAsString(), GameGetDTO.class);
    }

    // Helper to join/update a game via API - Returns ResultActions
    private ResultActions performUpdateGame(String token, Long gameId, double lat, double lon, boolean startGame) throws Exception {
        GamePutDTO gamePutDTO = new GamePutDTO();
        gamePutDTO.setLocationLat(lat);
        gamePutDTO.setLocationLong(lon);
        gamePutDTO.setStartGame(startGame);

        return mockMvc.perform(put("/games/{gameId}", gameId)
                .header("Authorization", token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(gamePutDTO)));
    }

    // Helper to admit caught via API - Returns ResultActions
    private ResultActions performAdmitCaught(String token, Long gameId, Long playerId) throws Exception {
        return mockMvc.perform(put("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", token));
    }

    // Helper to leave game via API - Returns ResultActions
    private ResultActions performLeaveGame(String token, Long gameId, Long playerId) throws Exception {
        return mockMvc.perform(delete("/games/{gameId}/players/{playerId}", gameId, playerId)
                .header("Authorization", token));
    }


    // --- Test Cases ---

    @Test
    public void createGame_validInput_createsGameAndPlayer() throws Exception {
        // Arrange
        performCreateUser("creator", "password");
        UserGetDTO loggedInUser = performLoginUser("creator", "password");
        String token = loggedInUser.getToken();

        // Act
        GameGetDTO createdGame = performCreateGame(token, "Test Game", 47.0, 8.0, 10.0, 30, 300);


        // Assert API Response
        assertEquals("Test Game", createdGame.getGamename());
        assertEquals(GameStatus.IN_LOBBY, createdGame.getStatus());
        assertNotNull(createdGame.getGameId());
        assertEquals(1, createdGame.getPlayers().size());
        assertEquals(loggedInUser.getUserId(), createdGame.getPlayers().get(0).getUserId());
        assertNotNull(createdGame.getPlayers().get(0).getPlayerId());

        // Assert Database State
        Optional<Game> dbGameOpt = gameRepository.findById(createdGame.getGameId());
        assertTrue(dbGameOpt.isPresent());
        Game dbGame = dbGameOpt.get();
        assertEquals("Test Game", dbGame.getGamename());
        assertEquals(GameStatus.IN_LOBBY, dbGame.getStatus());
        assertEquals(1, dbGame.getPlayers().size());
        assertEquals(loggedInUser.getUserId(), dbGame.getPlayers().get(0).getUser().getUserId());
        assertEquals(dbGame.getPlayers().get(0), dbGame.getCreator());

        Optional<Player> dbPlayerOpt = playerRepository.findById(createdGame.getPlayers().get(0).getPlayerId());
        assertTrue(dbPlayerOpt.isPresent());
        assertEquals(loggedInUser.getUserId(), dbPlayerOpt.get().getUser().getUserId());
    }

    @Test
    public void joinGame_validInput_addsPlayerToGame() throws Exception {
        // Arrange
        performCreateUser("creator", "password");
        performCreateUser("joiner", "password");
        UserGetDTO creator = performLoginUser("creator", "password");
        UserGetDTO joiner = performLoginUser("joiner", "password");
        GameGetDTO game = performCreateGame(creator.getToken(), "Join Test", 47.0, 8.0, 10.0, 30, 300);
        Long gameId = game.getGameId();

        // Act & Assert API Response
        performUpdateGame(joiner.getToken(), gameId, 47.1, 8.1, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.players", hasSize(2)))
                .andExpect(jsonPath("$.players[?(@.userId == " + joiner.getUserId() + ")]").exists());

        // Assert Database State
        Optional<Game> dbGameOpt = gameRepository.findById(gameId);
        assertTrue(dbGameOpt.isPresent());
        assertEquals(2, dbGameOpt.get().getPlayers().size());
        assertTrue(dbGameOpt.get().getPlayers().stream().anyMatch(p -> p.getUser().getUserId().equals(joiner.getUserId())));
    }

    @Test
    public void startGame_byCreatorWithEnoughPlayers_startsPreparation() throws Exception {
        // Arrange
        performCreateUser("creator", "password");
        performCreateUser("p2", "password");
        performCreateUser("p3", "password");
        UserGetDTO creator = performLoginUser("creator", "password");
        UserGetDTO p2 = performLoginUser("p2", "password");
        UserGetDTO p3 = performLoginUser("p3", "password");
        GameGetDTO game = performCreateGame(creator.getToken(), "Start Test", 47.0, 8.0, 10, 30, 300);
        Long gameId = game.getGameId();
        performUpdateGame(p2.getToken(), gameId, 47.1, 8.1, false).andExpect(status().isOk());
        performUpdateGame(p3.getToken(), gameId, 47.2, 8.2, false).andExpect(status().isOk());

        // Act & Assert API Response
        performUpdateGame(creator.getToken(), gameId, 47.0, 8.0, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(GameStatus.IN_GAME_PREPARATION.toString())))
                .andExpect(jsonPath("$.players[?(@.role == 'HUNTER')]", hasSize(1)))
                .andExpect(jsonPath("$.players[?(@.role == 'HIDER')]", hasSize(2)))
                .andExpect(jsonPath("$.radius", greaterThan(0.0)))
                .andExpect(jsonPath("$.timer").exists());

        // Assert Database State
        Optional<Game> dbGameOpt = gameRepository.findById(gameId);
        assertTrue(dbGameOpt.isPresent());
        assertEquals(GameStatus.IN_GAME_PREPARATION, dbGameOpt.get().getStatus());
        assertTrue(dbGameOpt.get().getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.HUNTER));
    }

    @Test
    public void admitCaught_lastHider_finishesGameAndUpdatesStats() throws Exception {
        // Arrange: Setup a game with 1 hunter, 1 hider, manually set to IN_GAME
        UserGetDTO creator = performCreateUser("creatorGH", "password"); // Hider
        UserGetDTO hunterUser = performCreateUser("hunterGH", "password"); // Hunter
        UserGetDTO loggedInCreator = performLoginUser("creatorGH", "password");
        UserGetDTO loggedInHunter = performLoginUser("hunterGH", "password");
        GameGetDTO gameDTO = performCreateGame(loggedInCreator.getToken(), "Finish Test", 47.0, 8.0, 10.0, 30, 300);
        Long gameId = gameDTO.getGameId();
        performUpdateGame(loggedInHunter.getToken(), gameId, 47.1, 8.1, false).andExpect(status().isOk());

        // Manually set roles and status for test predictability
        Game game = gameRepository.findById(gameId).get();
        Player creatorPlayer = game.getPlayers().stream().filter(p -> p.getUser().getUserId().equals(creator.getUserId())).findFirst().get();
        Player hunterPlayer = game.getPlayers().stream().filter(p -> p.getUser().getUserId().equals(hunterUser.getUserId())).findFirst().get();
        creatorPlayer.setRole(PlayerRole.HIDER);
        creatorPlayer.setStatus(PlayerStatus.HIDING);
        hunterPlayer.setRole(PlayerRole.HUNTER);
        hunterPlayer.setStatus(PlayerStatus.HUNTING);
        game.setStatus(GameStatus.IN_GAME); // Simulate timer fired
        gameRepository.saveAndFlush(game);
        Long hiderPlayerId = creatorPlayer.getPlayerId();

        // Act & Assert API Response
        performAdmitCaught(loggedInCreator.getToken(), gameId, hiderPlayerId)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is(GameStatus.FINISHED.toString())))
                .andExpect(jsonPath("$.players[?(@.playerId == " + hiderPlayerId + ")].status", contains(PlayerStatus.FOUND.toString())))
                .andExpect(jsonPath("$.players[?(@.role == 'HUNTER')].rank", contains(1.0))); // Hunter wins


        // Assert Database State (Stats and Decoupling)
        Optional<User> finalCreatorOpt = userRepository.findById(creator.getUserId());
        Optional<User> finalHunterOpt = userRepository.findById(hunterUser.getUserId());
        assertTrue(finalCreatorOpt.isPresent());
        assertTrue(finalHunterOpt.isPresent());
        assertEquals("1", finalHunterOpt.get().getStats().get("gamesPlayed"));
        assertEquals("1", finalHunterOpt.get().getStats().get("wins"));
        assertEquals("1", finalCreatorOpt.get().getStats().get("gamesPlayed"));
        assertEquals("0", finalCreatorOpt.get().getStats().getOrDefault("wins", "0"));

        Optional<Player> hiderPlayerDbOpt = playerRepository.findById(hiderPlayerId);
        Optional<Player> hunterPlayerDbOpt = playerRepository.findById(hunterPlayer.getPlayerId());
        assertTrue(hiderPlayerDbOpt.isPresent());
        assertTrue(hunterPlayerDbOpt.isPresent());
        assertNull(hiderPlayerDbOpt.get().getUser());
        assertNull(hunterPlayerDbOpt.get().getUser());
    }

    @Test
    public void leaveGame_nonCreatorInLobby_removesPlayer() throws Exception {
        // Arrange
        performCreateUser("creatorL", "password");
        performCreateUser("p2L", "password");
        UserGetDTO creator = performLoginUser("creatorL", "password");
        UserGetDTO p2 = performLoginUser("p2L", "password");
        GameGetDTO game = performCreateGame(creator.getToken(), "Leave Test", 47.0, 8.0, 10.0, 30, 300);
        Long gameId = game.getGameId();
        MvcResult joinResult = performUpdateGame(p2.getToken(), gameId, 47.1, 8.1, false)
                .andExpect(status().isOk())
                .andReturn();
        GameGetDTO gameAfterJoin = objectMapper.readValue(joinResult.getResponse().getContentAsString(), GameGetDTO.class);
        Long p2PlayerId = gameAfterJoin.getPlayers().stream().filter(p -> p.getUserId().equals(p2.getUserId())).findFirst().get().getPlayerId();

        // Act & Assert API Response
        performLeaveGame(p2.getToken(), gameId, p2PlayerId)
                .andExpect(status().isNoContent());


        // Assert Database State
        Optional<Game> dbGameOpt = gameRepository.findById(gameId);
        assertTrue(dbGameOpt.isPresent());
        assertEquals(1, dbGameOpt.get().getPlayers().size());
        assertFalse(dbGameOpt.get().getPlayers().stream().anyMatch(p -> p.getPlayerId().equals(p2PlayerId)));

        Optional<Player> p2PlayerOpt = playerRepository.findById(p2PlayerId);
        assertFalse(p2PlayerOpt.isPresent());
    }

    @Test
    public void leaveGame_creatorInLobby_deletesGame() throws Exception {
        // Arrange
        performCreateUser("creatorD", "password");
        UserGetDTO creator = performLoginUser("creatorD", "password");
        GameGetDTO game = performCreateGame(creator.getToken(), "Delete Test", 47.0, 8.0, 10.0, 30, 300);
        Long gameId = game.getGameId();
        Long creatorPlayerId = game.getPlayers().get(0).getPlayerId();

        // Act & Assert API Response
        performLeaveGame(creator.getToken(), gameId, creatorPlayerId)
                .andExpect(status().isNoContent());


        // Assert Database State
        Optional<Game> dbGameOpt = gameRepository.findById(gameId);
        assertFalse(dbGameOpt.isPresent());

        Optional<Player> creatorPlayerOpt = playerRepository.findById(creatorPlayerId);
        assertFalse(creatorPlayerOpt.isPresent());
    }

}
