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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Test class for the GameService.
 *
 * @see GameService
 */
public class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserService userService; // Currently unused mock, but might be needed if GameService calls UserService

    @Mock
    private PlayerRepository playerRepository;

    @Mock
    private GameTimerService gameTimerService;

    @InjectMocks
    private GameService gameService;

    // Test fixtures
    private User testUserCreator;
    private Player testPlayerCreator;
    private User testUserJoiner;
    private Game testGame;
    private GamePutDTO testGamePutDTO;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Initialize test users
        testUserCreator = new User();
        testUserCreator.setUserId(1L);
        testUserCreator.setUsername("creator");
        testUserCreator.setToken("creator-token");
        testUserCreator.setStats(new HashMap<>());

        testUserJoiner = new User();
        testUserJoiner.setUserId(2L);
        testUserJoiner.setUsername("joiner");
        testUserJoiner.setToken("joiner-token");
        testUserJoiner.setStats(new HashMap<>());

        // Initialize test player (linked to creator user)
        testPlayerCreator = new Player();
        testPlayerCreator.setPlayerId(10L);
        testPlayerCreator.setUser(testUserCreator);
        testPlayerCreator.setDisplayName(testUserCreator.getUsername());
        testPlayerCreator.setLocationLat(47.0);
        testPlayerCreator.setLocationLong(8.0);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);
        testPlayerCreator.setRole(PlayerRole.HIDER);

        // Initialize test game
        testGame = new Game();
        testGame.setGameId(100L);
        testGame.setGamename("Test Game");
        testGame.setStatus(GameStatus.IN_LOBBY);
        testGame.setCreator(testPlayerCreator);
        testGame.setPlayers(new ArrayList<>(List.of(testPlayerCreator))); // Use mutable list
        testPlayerCreator.setGame(testGame); // Set bidirectional relationship

        // Initialize test DTO
        testGamePutDTO = new GamePutDTO();
        testGamePutDTO.setLocationLat(47.1);
        testGamePutDTO.setLocationLong(8.1);
        testGamePutDTO.setStartGame(false);

        // Default mock behaviors using lenient() to avoid warnings for unused stubs
        lenient().when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(gameRepository.save(any(Game.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(playerRepository.findPlayerByPlayerId(testPlayerCreator.getPlayerId())).thenReturn(testPlayerCreator);
        lenient().when(gameRepository.findByGameId(testGame.getGameId())).thenReturn(testGame);
        lenient().when(playerRepository.findByUser(any(User.class))).thenReturn(Optional.empty());
        lenient().when(gameRepository.findByStatusNot(GameStatus.FINISHED)).thenReturn(Collections.emptyList());

        // Mock timer service to do nothing by default
        lenient().doNothing().when(gameTimerService).startPreparationTimer(anyLong());
        lenient().doNothing().when(gameTimerService).startFinishTimer(anyLong());
        lenient().doNothing().when(gameTimerService).stopFinishTimer(anyLong());

        // Manually inject gameTimerService mock because @Autowired might not work with @InjectMocks
        ReflectionTestUtils.setField(gameService, "gameTimerService", gameTimerService);
    }

    // --- createPlayer Tests ---

    @Test
    public void createPlayer_validInput_success() {
        // given
        double lat = 47.5;
        double lon = 8.5;
        given(playerRepository.findByUser(testUserCreator)).willReturn(Optional.empty());

        // when
        Player createdPlayer = gameService.createPlayer(lat, lon, testUserCreator);

        // then
        verify(playerRepository, times(1)).findByUser(eq(testUserCreator));
        verify(playerRepository, times(1)).save(any(Player.class));
        verify(playerRepository, times(1)).flush();

        assertNotNull(createdPlayer);
        assertEquals(testUserCreator.getUsername(), createdPlayer.getDisplayName());
        assertEquals(lat, createdPlayer.getLocationLat());
        assertEquals(lon, createdPlayer.getLocationLong());
        assertEquals(testUserCreator, createdPlayer.getUser());
    }

    @Test
    public void createPlayer_userAlreadyPlayer_throwsConflict() {
        // given
        given(playerRepository.findByUser(testUserCreator)).willReturn(Optional.of(testPlayerCreator));

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.createPlayer(47.5, 8.5, testUserCreator);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("User is already a Player"));
        verify(playerRepository, never()).save(any(Player.class));
    }


    // --- createGame Tests ---
    @Test
    public void createGame_validInput_success() {
        // given
        String newGameName = "Unique Game";
        given(gameRepository.findByStatusNot(GameStatus.FINISHED)).willReturn(Collections.emptyList());

        // when
        Game createdGame = gameService.createGame(newGameName, testPlayerCreator);

        // then
        verify(gameRepository, times(1)).findByStatusNot(eq(GameStatus.FINISHED));
        verify(gameRepository, times(1)).save(any(Game.class));
        verify(gameRepository, times(1)).flush();

        assertNotNull(createdGame);
        assertEquals(newGameName, createdGame.getGamename());
        assertEquals(GameStatus.IN_LOBBY, createdGame.getStatus());
        assertEquals(testPlayerCreator, createdGame.getCreator());
        assertTrue(createdGame.getPlayers().contains(testPlayerCreator));
        assertEquals(createdGame, testPlayerCreator.getGame());
    }

    @Test
    public void createGame_gamenameConflict_throwsConflict() {
        // given
        String conflictingName = "Existing Game";
        Game conflictingGame = new Game();
        conflictingGame.setGamename(conflictingName);
        given(gameRepository.findByStatusNot(GameStatus.FINISHED)).willReturn(List.of(conflictingGame));

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.createGame(conflictingName, testPlayerCreator);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("Gamename already exists"));
        verify(gameRepository, never()).save(any(Game.class));
    }

    // --- getJoinableGames Tests ---
    @Test
    public void getJoinableGames_returnsInLobbyGames() {
        // given
        Game lobbyGame1 = new Game(); lobbyGame1.setStatus(GameStatus.IN_LOBBY);
        Game lobbyGame2 = new Game(); lobbyGame2.setStatus(GameStatus.IN_LOBBY);
        List<Game> expectedGames = List.of(lobbyGame1, lobbyGame2);
        given(gameRepository.findByStatus(GameStatus.IN_LOBBY)).willReturn(expectedGames);

        // when
        List<Game> actualGames = gameService.getJoinableGames();

        // then
        verify(gameRepository, times(1)).findByStatus(eq(GameStatus.IN_LOBBY));
        assertEquals(expectedGames, actualGames);
    }


    // --- updateGame / handleInLobby Tests ---

    @Test
    public void updateGame_lobby_joinSuccess() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        testGame.getPlayers().clear(); // Start with empty game for join test
        testGame.getPlayers().add(testPlayerCreator); // Add creator back
        assertEquals(1, testGame.getPlayers().size());

        Player joiningPlayer = new Player(); joiningPlayer.setUser(testUserJoiner); joiningPlayer.setPlayerId(11L);
        given(playerRepository.findByUser(testUserJoiner)).willReturn(Optional.empty());
        when(playerRepository.save(argThat(p -> p.getUser().equals(testUserJoiner)))).thenReturn(joiningPlayer);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserJoiner, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(playerRepository, times(1)).save(argThat(p -> p.getUser().equals(testUserJoiner)));
        verify(playerRepository, times(1)).flush(); // From createPlayer
        verify(gameRepository, times(1)).save(testGame); // From handleInLobby
        verify(gameRepository, times(1)).flush(); // From handleInLobby

        assertEquals(2, updatedGame.getPlayers().size());
        Player addedPlayer = updatedGame.getPlayers().stream().filter(p -> p.getUser().equals(testUserJoiner)).findFirst().orElse(null);
        assertNotNull(addedPlayer);
        assertEquals(updatedGame, addedPlayer.getGame());
    }


    @Test
    public void updateGame_lobby_joinFailsGameFull() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        // Fill the game (assume max 5 players)
        for (int i = 0; i < 4; i++) {
            Player p = new Player(); p.setPlayerId(20L + i); User u = new User(); u.setUserId(20L+i); p.setUser(u);
            testGame.addPlayer(p);
        }
        assertEquals(5, testGame.getPlayers().size());
        given(playerRepository.findByUser(testUserJoiner)).willReturn(Optional.empty());

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(testGame.getGameId(), testUserJoiner, testGamePutDTO);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Game is full"));
        verify(gameRepository, never()).save(any(Game.class));
        verify(playerRepository, never()).save(any(Player.class));
    }

    @Test
    public void updateGame_lobby_updateLocationSuccess() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        assertTrue(testGame.getPlayers().contains(testPlayerCreator));
        double newLat = 47.5;
        double newLng = 8.5;
        testGamePutDTO.setLocationLat(newLat);
        testGamePutDTO.setLocationLong(newLng);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(playerRepository, never()).save(any(Player.class)); // No new player created
        verify(gameRepository, times(1)).save(testGame); // Game state saved
        verify(gameRepository, times(1)).flush();

        assertEquals(1, updatedGame.getPlayers().size());
        assertEquals(newLat, updatedGame.getPlayers().get(0).getLocationLat());
        assertEquals(newLng, updatedGame.getPlayers().get(0).getLocationLong());
    }

    @Test
    public void updateGame_lobby_startGameSuccess() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        // Add enough players (>2 total)
        Player player2 = new Player(); player2.setPlayerId(11L); player2.setUser(testUserJoiner);
        testGame.addPlayer(player2);
        User user3 = new User(); user3.setUserId(3L); user3.setStats(new HashMap<>());
        Player player3 = new Player(); player3.setPlayerId(12L); player3.setUser(user3);
        testGame.addPlayer(player3);
        assertEquals(3, testGame.getPlayers().size());

        testGamePutDTO.setStartGame(true);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        // Expect 3 saves/flushes due to fall-through logic:
        // 1. handleInLobby (location update) -> save/flush
        // 2. handleInLobby -> start -> save/flush
        // 3. handleInGamePreparation (location update) -> save/flush
        verify(gameRepository, times(3)).save(testGame);
        verify(gameRepository, times(3)).flush();
        verify(gameTimerService, times(1)).startPreparationTimer(eq(testGame.getGameId()));

        assertEquals(GameStatus.IN_GAME_PREPARATION, updatedGame.getStatus());
        assertNotNull(updatedGame.getTimer());
        assertTrue(updatedGame.getRadius() > 0.0);
        assertTrue(updatedGame.getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.HUNTER));
        assertTrue(updatedGame.getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.HIDER));
    }

    @Test
    public void updateGame_lobby_startGameFailNotCreator() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        Player player2 = new Player(); player2.setPlayerId(11L); player2.setUser(testUserJoiner);
        testGame.addPlayer(player2);
        User user3 = new User(); user3.setUserId(3L); user3.setStats(new HashMap<>());
        Player player3 = new Player(); player3.setPlayerId(12L); player3.setUser(user3);
        testGame.addPlayer(player3);

        testGamePutDTO.setStartGame(true);

        // when / then: User testUserJoiner (not creator) tries to start
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(testGame.getGameId(), testUserJoiner, testGamePutDTO);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Only the creator can start the game"));
        // Verify save/flush are NOT called on game repo because exception is thrown before end of method
        verify(gameRepository, never()).save(testGame);
        verify(gameRepository, never()).flush();
        verify(gameTimerService, never()).startPreparationTimer(anyLong());
        // Player repo might be called if joiner wasn't already in game (not this case)
        verify(playerRepository, atMostOnce()).save(any(Player.class));
    }

    @Test
    public void updateGame_lobby_startGameFailNotEnoughPlayers() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        // Only creator and one other player (total 2)
        Player player2 = new Player(); player2.setPlayerId(11L); player2.setUser(testUserJoiner);
        testGame.addPlayer(player2);
        assertEquals(2, testGame.getPlayers().size());

        testGamePutDTO.setStartGame(true);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Only the creator can start the game"));
        // Verify save/flush are NOT called on game repo because exception is thrown before end of method
        verify(gameRepository, never()).save(testGame);
        verify(gameRepository, never()).flush();
        verify(gameTimerService, never()).startPreparationTimer(anyLong());
    }

    // --- updateGame / handleInGamePreparation Tests ---
    @Test
    public void updateGame_preparation_updateLocationSuccess() {
        // given
        testGame.setStatus(GameStatus.IN_GAME_PREPARATION);
        double newLat = 47.5;
        double newLng = 8.5;
        testGamePutDTO.setLocationLat(newLat);
        testGamePutDTO.setLocationLong(newLng);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(gameRepository, times(1)).save(testGame);
        verify(gameRepository, times(1)).flush();

        assertEquals(newLat, updatedGame.getPlayers().get(0).getLocationLat());
        assertEquals(newLng, updatedGame.getPlayers().get(0).getLocationLong());
        assertEquals(GameStatus.IN_GAME_PREPARATION, updatedGame.getStatus());
    }

    @Test
    public void updateGame_preparation_updateLocationFailNotJoined() {
        // given
        testGame.setStatus(GameStatus.IN_GAME_PREPARATION);
        double newLat = 47.5;
        double newLng = 8.5;
        testGamePutDTO.setLocationLat(newLat);
        testGamePutDTO.setLocationLong(newLng);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(testGame.getGameId(), testUserJoiner, testGamePutDTO); // Use joiner who is not in game
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Game has already started"));
        verify(gameRepository, never()).save(any(Game.class));
    }

    // --- updateGame / handleInGame Tests ---
    @Test
    public void updateGame_inGame_updateLocationSuccess_inArea() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testGame.setCenterLatitude(47.0);
        testGame.setCenterLongitude(8.0);
        testGame.setRadius(1000); // 1km radius

        double newLat = 47.001; // Close to center
        double newLng = 8.001;
        testGamePutDTO.setLocationLat(newLat);
        testGamePutDTO.setLocationLong(newLng);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(gameRepository, times(1)).save(testGame);
        verify(gameRepository, times(1)).flush();

        Player updatedPlayer = updatedGame.getPlayers().stream().filter(p->p.getUser().equals(testUserCreator)).findFirst().get();
        assertEquals(newLat, updatedPlayer.getLocationLat());
        assertEquals(newLng, updatedPlayer.getLocationLong());
        assertFalse(updatedPlayer.isOutOfArea());
        assertEquals(GameStatus.IN_GAME, updatedGame.getStatus());
    }

    @Test
    public void updateGame_inGame_updateLocationSuccess_outOfArea() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testGame.setCenterLatitude(47.0);
        testGame.setCenterLongitude(8.0);
        testGame.setRadius(100); // 100m radius

        double newLat = 48.0; // Far from center
        double newLng = 9.0;
        testGamePutDTO.setLocationLat(newLat);
        testGamePutDTO.setLocationLong(newLng);

        // when
        Game updatedGame = gameService.updateGame(testGame.getGameId(), testUserCreator, testGamePutDTO);

        // then
        verify(gameRepository, times(1)).save(testGame);
        Player updatedPlayer = updatedGame.getPlayers().stream().filter(p->p.getUser().equals(testUserCreator)).findFirst().get();
        assertEquals(newLat, updatedPlayer.getLocationLat());
        assertEquals(newLng, updatedPlayer.getLocationLong());
        assertTrue(updatedPlayer.isOutOfArea());
    }

    @Test
    public void updateGame_inGame_updateLocationFailNotJoined() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(testGame.getGameId(), testUserJoiner, testGamePutDTO); // Use joiner who is not in game
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Game has already started"));
        verify(gameRepository, never()).save(any(Game.class));
    }

    // --- updateGame General Error ---
    @Test
    public void updateGame_gameNotFound_throwsNotFound() {
        // given
        long nonExistentGameId = 999L;
        given(gameRepository.findByGameId(nonExistentGameId)).willReturn(null);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGame(nonExistentGameId, testUserCreator, testGamePutDTO);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getReason().contains("Game not found"));
    }

    // --- updatePlayer Tests ---

    @Test
    public void updatePlayer_admitCaught_success() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);

        // Add another HIDER to prevent game from finishing immediately
        User user3 = new User(); user3.setUserId(3L); user3.setStats(new HashMap<>());
        Player otherHider = new Player(); otherHider.setPlayerId(12L); otherHider.setUser(user3);
        otherHider.setRole(PlayerRole.HIDER); otherHider.setStatus(PlayerStatus.HIDING);
        testGame.addPlayer(otherHider);

        // Add a hunter for realism
        Player hunter = new Player(); hunter.setPlayerId(11L); hunter.setUser(testUserJoiner); hunter.setRole(PlayerRole.HUNTER); hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when
        Game updatedGame = gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);

        // then
        // findByGameId only called once in updatePlayer (since game doesn't end, updateStats isn't called)
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(playerRepository, times(1)).findPlayerByPlayerId(testPlayerCreator.getPlayerId());
        verify(gameRepository, times(1)).save(testGame); // Only saved once in updatePlayer
        verify(userRepository, never()).save(any(User.class)); // updateStats not reached
        verify(gameRepository, times(1)).flush(); // Only flushed once in updatePlayer
        verify(gameTimerService, never()).stopFinishTimer(testGame.getGameId()); // Game didn't finish

        Player updatedPlayer = updatedGame.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(testPlayerCreator.getPlayerId()))
                .findFirst().get();
        assertEquals(PlayerStatus.FOUND, updatedPlayer.getStatus());
        assertNotNull(updatedPlayer.getFoundTime());
    }

    @Test
    public void updatePlayer_admitCaught_success_gameFinished() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);

        // This player is the only hider
        Player hunter = new Player(); hunter.setPlayerId(11L); hunter.setUser(testUserJoiner); hunter.setRole(PlayerRole.HUNTER); hunter.setStatus(PlayerStatus.HUNTING);
        testGame.setPlayers(new ArrayList<>(List.of(testPlayerCreator, hunter))); // Only 1 hider and 1 hunter

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when
        Game updatedGame = gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);

        // then
        verify(gameRepository, times(2)).findByGameId(testGame.getGameId()); // updatePlayer + updateStats
        verify(gameRepository, times(2)).save(testGame); // updatePlayer + finishGame
        verify(gameRepository, times(2)).flush(); // updatePlayer + finishGame
        verify(userRepository, atLeastOnce()).save(any(User.class)); // Stats update
        verify(userRepository, atLeastOnce()).flush(); // Stats update flush
        verify(gameTimerService, times(1)).stopFinishTimer(testGame.getGameId()); // Timer stopped

        assertEquals(GameStatus.FINISHED, updatedGame.getStatus());
        Player caughtPlayer = updatedGame.getPlayers().stream().filter(p->p.getPlayerId().equals(testPlayerCreator.getPlayerId())).findFirst().orElse(null);
        Player hunterPlayer = updatedGame.getPlayers().stream().filter(p->p.getPlayerId().equals(hunter.getPlayerId())).findFirst().orElse(null);

        assertNotNull(caughtPlayer);
        assertEquals(PlayerStatus.FOUND, caughtPlayer.getStatus());
        assertNotNull(caughtPlayer.getFoundTime());
        assertNull(caughtPlayer.getUser()); // Check decoupling
        assertNotNull(hunterPlayer);
        assertNull(hunterPlayer.getUser()); // Check decoupling
    }


    @Test
    public void updatePlayer_fail_gameNotFound() {
        // given
        long nonExistentGameId = 999L;
        given(gameRepository.findByGameId(nonExistentGameId)).willReturn(null);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(nonExistentGameId, testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void updatePlayer_fail_playerNotFound() {
        // given
        long nonExistentPlayerId = 999L;
        given(playerRepository.findPlayerByPlayerId(nonExistentPlayerId)).willReturn(null);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), nonExistentPlayerId, testUserCreator);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void updatePlayer_fail_forbidden_playerNotInGame() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        Player playerToRemove = testGame.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(testPlayerCreator.getPlayerId()))
                .findFirst().orElse(null);
        if (playerToRemove != null) {
            testGame.getPlayers().remove(playerToRemove);
        }

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Player is not in that game"));
    }

    @Test
    public void updatePlayer_fail_forbidden_userNotPlayer() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserJoiner); // Wrong user
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("user is not that player"));
    }

    @Test
    public void updatePlayer_fail_forbidden_gameNotInGame() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Player is not in that game"));
    }

    @Test
    public void updatePlayer_fail_forbidden_playerNotHider() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HUNTER);
        testPlayerCreator.setStatus(PlayerStatus.HUNTING);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Player is not a hider or already found"));
    }

    @Test
    public void updatePlayer_fail_forbidden_playerAlreadyFound() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.FOUND);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updatePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Player is not a hider or already found"));
    }


    // --- deletePlayer Tests ---
    @Test
    public void deletePlayer_leaveLobby_notCreator_success() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        Player leavingPlayer = new Player(); leavingPlayer.setPlayerId(11L); leavingPlayer.setUser(testUserJoiner);
        testGame.addPlayer(leavingPlayer);
        given(playerRepository.findPlayerByPlayerId(11L)).willReturn(leavingPlayer);

        // when
        gameService.deletePlayer(testGame.getGameId(), 11L, testUserJoiner);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(playerRepository, times(1)).findPlayerByPlayerId(11L);
        verify(gameRepository, times(1)).save(testGame); // Game saved after player removal
        verify(gameRepository, times(1)).flush();
        verify(gameRepository, never()).delete(any(Game.class));

        assertEquals(1, testGame.getPlayers().size());
        assertFalse(testGame.getPlayers().contains(leavingPlayer));
    }

    @Test
    public void deletePlayer_leaveLobby_creator_success_deletesGame() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);

        // when
        gameService.deletePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);

        // then
        verify(gameRepository, times(1)).findByGameId(testGame.getGameId());
        verify(playerRepository, times(1)).findPlayerByPlayerId(testPlayerCreator.getPlayerId());
        verify(gameRepository, times(1)).delete(testGame); // Game deleted
        verify(gameRepository, never()).save(any(Game.class));
        verify(gameRepository, never()).flush();
    }


    @Test
    public void deletePlayer_fail_gameNotFound() {
        // given
        long nonExistentGameId = 999L;
        given(gameRepository.findByGameId(nonExistentGameId)).willReturn(null);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.deletePlayer(nonExistentGameId, testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void deletePlayer_fail_playerNotFound() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        long nonExistentPlayerId = 999L;
        given(playerRepository.findPlayerByPlayerId(nonExistentPlayerId)).willReturn(null);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.deletePlayer(testGame.getGameId(), nonExistentPlayerId, testUserCreator);
        });
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
    }

    @Test
    public void deletePlayer_fail_gameNotInLobby() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.deletePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("Game has already started"));
    }

    @Test
    public void deletePlayer_fail_forbidden_playerNotInGame() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        Player playerToRemove = testGame.getPlayers().stream()
                .filter(p -> p.getPlayerId().equals(testPlayerCreator.getPlayerId()))
                .findFirst().orElse(null);
        if (playerToRemove != null) {
            testGame.getPlayers().remove(playerToRemove);
        }

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.deletePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserCreator);
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Player is not in that game"));
    }

    @Test
    public void deletePlayer_fail_forbidden_userNotPlayer() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);

        // when / then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.deletePlayer(testGame.getGameId(), testPlayerCreator.getPlayerId(), testUserJoiner); // Wrong user
        });
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("User is not that player"));
    }

    // --- calculateDistance Test (Example) ---
    @Test
    public void calculateDistance_knownPoints_returnsCorrectDistance() {
        // Zurich approx coords
        double lat1 = 47.3769;
        double lon1 = 8.5417;
        // Bern approx coords
        double lat2 = 46.9480;
        double lon2 = 7.4474;

        // Expected distance ~95-96km (rough) -> 95000-96000m
        double expectedMin = 95000 - 5; // Account for 5m tolerance subtraction
        double expectedMax = 96000 - 5;

        double distance = gameService.calculateDistance(lat1, lon1, lat2, lon2);

        assertTrue(distance > expectedMin && distance < expectedMax,
                "Distance Zurich-Bern (" + distance + "m) should be ~95-96km (minus 5m tolerance)");
    }

}
