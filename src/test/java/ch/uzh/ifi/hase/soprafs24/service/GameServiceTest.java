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
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameCenterDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
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
import java.util.*;

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
    private GamePostDTO testGamePostDTO;


    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);

        // Initialize test users
        testUserCreator = new User();
        testUserCreator.setUserId(1L);
        testUserCreator.setUsername("creator");
        testUserCreator.setPassword("creator_password");
        testUserCreator.setProfilePicture("https://ui-avatars.com/api/?name=creator&length=1&rounded=true&size=128");
        testUserCreator.setToken("creator-token");
        testUserCreator.setStats(new HashMap<>());

        testUserJoiner = new User();
        testUserJoiner.setUserId(2L);
        testUserJoiner.setUsername("joiner");
        testUserJoiner.setPassword("joiner_password");
        testUserJoiner.setProfilePicture("https://ui-avatars.com/api/?name=joiner&length=1&rounded=true&size=128");
        testUserJoiner.setToken("joiner-token");
        testUserJoiner.setStats(new HashMap<>());

        // Initialize test player (linked to creator user)
        testPlayerCreator = new Player();
        testPlayerCreator.setPlayerId(10L);
        testPlayerCreator.setUser(testUserCreator);
        testPlayerCreator.setDisplayName(testUserCreator.getUsername());
        testPlayerCreator.setDisplayPicture(testUserCreator.getProfilePicture());
        testPlayerCreator.setLocationLat(47.0);
        testPlayerCreator.setLocationLong(8.0);


        // Initialize test game
        testGame = new Game();
        testGame.setGameId(100L);
        testGame.setGamename("Test Game");
        testGame.setStatus(GameStatus.IN_LOBBY);
        testGame.setCreator(testPlayerCreator);
        testGame.setPlayers(new ArrayList<>(List.of(testPlayerCreator))); // Use mutable list
        testPlayerCreator.setGame(testGame); // Set bidirectional relationship

        // Initialize GamePostDTO for createGame tests
        testGamePostDTO = new GamePostDTO();
        testGamePostDTO.setGamename(testGame.getGamename());
        testGamePostDTO.setLocationLat(testGame.getCenterLatitude());
        testGamePostDTO.setLocationLong(testGame.getCenterLongitude());
        testGamePostDTO.setRadius(10.0);
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(300);

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
        lenient().doNothing().when(gameTimerService).startPreparationTimer(anyLong(), anyInt(), anyInt());
        lenient().doNothing().when(gameTimerService).startFinishTimer(anyLong(), anyInt());
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
        assertEquals(testUserCreator.getProfilePicture(), createdPlayer.getDisplayPicture());
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
        testGamePostDTO.setGamename(newGameName);
        Game createdGame = gameService.createGame(testGamePostDTO, testUserCreator);
        Player returned_Player = createdGame.getPlayers().get(0);

        // then
        verify(gameRepository, times(1)).findByStatusNot(eq(GameStatus.FINISHED));
        verify(gameRepository, times(1)).save(any(Game.class));
        verify(gameRepository, times(1)).flush();

        assertNotNull(createdGame);
        assertEquals(newGameName, createdGame.getGamename());
        assertEquals(GameStatus.IN_LOBBY, createdGame.getStatus());
        assertEquals(testGamePostDTO.getRadius(), createdGame.getRadius());
        assertEquals(testGamePostDTO.getPreparationTimeInSeconds(), createdGame.getPreparationTimeInSeconds());
        assertEquals(testGamePostDTO.getGameTimeInSeconds(), createdGame.getGameTimeInSeconds());

        assertEquals(testUserCreator.getUserId(), returned_Player.getUser().getUserId());
        assertEquals(testUserCreator.getUsername(), returned_Player.getDisplayName());
        assertEquals(testUserCreator.getProfilePicture(), returned_Player.getDisplayPicture());
        assertEquals(1, createdGame.getPlayers().size());


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
            testGamePostDTO.setGamename(conflictingName);
            gameService.createGame(testGamePostDTO, testUserCreator);
        });

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        assertTrue(exception.getReason().contains("Gamename already exists"));
        verify(gameRepository, never()).save(any(Game.class));
    }

    @Test
    public void createGame_nullTimes_useDefaults() {
        // given: leave times null to trigger defaults
        testGamePostDTO.setPreparationTimeInSeconds(null);
        testGamePostDTO.setGameTimeInSeconds(null);

        // when
        Game createdGame = gameService.createGame(testGamePostDTO, testUserCreator);

        // then
        assertEquals(30, createdGame.getPreparationTimeInSeconds());
        assertEquals(300, createdGame.getGameTimeInSeconds());
    }

    @Test
    public void createGame_preparationTimeTooShort_throwsBadRequest() {
        // given: prep time below minimum
        testGamePostDTO.setPreparationTimeInSeconds(5);
        testGamePostDTO.setGameTimeInSeconds(300);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Preparation time must be between 10 and 300 seconds"));
    }

    @Test
    public void createGame_preparationTimeTooLong_throwsBadRequest() {
        // given: prep time above maximum
        testGamePostDTO.setPreparationTimeInSeconds(301);
        testGamePostDTO.setGameTimeInSeconds(300);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Preparation time must be between 10 and 300 seconds"));
    }

    @Test
    public void createGame_gameTimeTooShort_throwsBadRequest() {
        // given: game time below minimum
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(30);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Game time must be between 60 and 900 seconds"));
    }

    @Test
    public void createGame_gameTimeTooLong_throwsBadRequest() {
        // given: game time above maximum
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(901);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Game time must be between 60 and 900 seconds"));
    }

    @Test
    public void createGame_radiusTooSmall_throwsBadRequest() {
        // given: radius below minimum
        testGamePostDTO.setRadius(4.9);
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(300);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Radius must be between 5 and 100 meters"));
    }

    @Test
    public void createGame_radiusTooLarge_throwsBadRequest() {
        // given: radius above maximum
        testGamePostDTO.setRadius(100.1);
        testGamePostDTO.setPreparationTimeInSeconds(30);
        testGamePostDTO.setGameTimeInSeconds(300);

        // when / then
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> gameService.createGame(testGamePostDTO, testUserCreator)
        );
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        assertTrue(exception.getReason().contains("Radius must be between 5 and 100 meters"));
    }

    // --- getJoinableGames Tests ---
    @Test
    public void getJoinableGames_returnsInLobbyGames() {
        // given
        Game lobbyGame1 = new Game();
        lobbyGame1.setStatus(GameStatus.IN_LOBBY);
        Game lobbyGame2 = new Game();
        lobbyGame2.setStatus(GameStatus.IN_LOBBY);
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

        Player joiningPlayer = new Player();
        joiningPlayer.setUser(testUserJoiner);
        joiningPlayer.setPlayerId(11L);
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
            Player p = new Player();
            p.setPlayerId(20L + i);
            User u = new User();
            u.setUserId(20L + i);
            p.setUser(u);
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
        Player player2 = new Player();
        player2.setPlayerId(11L);
        player2.setUser(testUserJoiner);
        testGame.addPlayer(player2);
        User user3 = new User();
        user3.setUserId(3L);
        user3.setStats(new HashMap<>());
        Player player3 = new Player();
        player3.setPlayerId(12L);
        player3.setUser(user3);
        testGame.addPlayer(player3);
        assertEquals(3, testGame.getPlayers().size());

        testGame.setRadius(testGamePostDTO.getRadius());
        double radiusPerPlayer = testGame.getRadius();
        testGame.setGameTimeInSeconds(testGamePostDTO.getGameTimeInSeconds());
        testGame.setPreparationTimeInSeconds(testGamePostDTO.getPreparationTimeInSeconds());

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
        verify(gameTimerService, times(1)).startPreparationTimer(eq(testGame.getGameId()), anyInt(), anyInt());

        assertEquals(GameStatus.IN_GAME_PREPARATION, updatedGame.getStatus());
        assertNotNull(updatedGame.getTimer());
        assertEquals(updatedGame.getRadius(), radiusPerPlayer * testGame.getPlayers().size());
        assertTrue(updatedGame.getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.HUNTER));
        assertTrue(updatedGame.getPlayers().stream().anyMatch(p -> p.getRole() == PlayerRole.HIDER));
    }

    @Test
    public void updateGame_lobby_startGameFailNotCreator() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        Player player2 = new Player();
        player2.setPlayerId(11L);
        player2.setUser(testUserJoiner);
        testGame.addPlayer(player2);
        User user3 = new User();
        user3.setUserId(3L);
        user3.setStats(new HashMap<>());
        Player player3 = new Player();
        player3.setPlayerId(12L);
        player3.setUser(user3);
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
        verify(gameTimerService, never()).startPreparationTimer(anyLong(), anyInt(), anyInt());
        // Player repo might be called if joiner wasn't already in game (not this case)
        verify(playerRepository, atMostOnce()).save(any(Player.class));
    }

    @Test
    public void updateGame_lobby_startGameFailNotEnoughPlayers() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        // Only creator

        assertEquals(1, testGame.getPlayers().size());

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
        verify(gameTimerService, never()).startPreparationTimer(anyLong(), anyInt(), anyInt());
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

        Player updatedPlayer = updatedGame.getPlayers().stream().filter(p -> p.getUser().equals(testUserCreator)).findFirst().get();
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
        Player updatedPlayer = updatedGame.getPlayers().stream().filter(p -> p.getUser().equals(testUserCreator)).findFirst().get();
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
    public void updateHider_admitCaught_success() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);

        // Add another HIDER to prevent game from finishing immediately
        User user3 = new User();
        user3.setUserId(3L);
        user3.setStats(new HashMap<>());
        Player otherHider = new Player();
        otherHider.setPlayerId(12L);
        otherHider.setUser(user3);
        otherHider.setRole(PlayerRole.HIDER);
        otherHider.setStatus(PlayerStatus.HIDING);
        testGame.addPlayer(otherHider);

        // Add a hunter for realism
        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
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
    public void updateHider_admitCaught_success_gameFinished() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);

        // This player is the only hider
        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
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
        Player caughtPlayer = updatedGame.getPlayers().stream().filter(p -> p.getPlayerId().equals(testPlayerCreator.getPlayerId())).findFirst().orElse(null);
        Player hunterPlayer = updatedGame.getPlayers().stream().filter(p -> p.getPlayerId().equals(hunter.getPlayerId())).findFirst().orElse(null);

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
    public void updateHunter_success_gameFinished() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);


        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);


        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);
        given(playerRepository.findPlayerByPlayerId(hunter.getPlayerId())).willReturn(hunter);


        doAnswer(invocation -> {
            Game game = invocation.getArgument(0);
            for (Player p : game.getPlayers()) {
                p.setUser(null);
            }
            return game;
        }).when(gameRepository).save(any(Game.class));

        // when
        Game updatedGame = gameService.updatePlayer(testGame.getGameId(), hunter.getPlayerId(), testUserJoiner);

        // then
        verify(gameTimerService, times(1)).stopFinishTimer(testGame.getGameId());
        assertEquals(GameStatus.FINISHED, updatedGame.getStatus());


        for (Player p : updatedGame.getPlayers()) {
            assertNull(p.getUser());
        }
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
        assertTrue(exception.getReason().contains("Player is already found"));
    }


    // --- deletePlayer Tests ---
    @Test
    public void deletePlayer_leaveLobby_notCreator_success() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);
        Player leavingPlayer = new Player();
        leavingPlayer.setPlayerId(11L);
        leavingPlayer.setUser(testUserJoiner);
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

    // --- updateGameCenter Tests ---
    @Test
    public void updateGameCenter_userNotInGame_Forbidden() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);


        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.0005);
        centerDTO.setLongitude(8.0005);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGameCenter(testGame.getGameId(), centerDTO, testUserJoiner);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("User is not in game"));
    }

    @Test
    public void updateGameCenter_userNotHunter_Forbidden() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        testPlayerCreator.setRole(PlayerRole.HIDER);

        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.0005);
        centerDTO.setLongitude(8.0005);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGameCenter(testGame.getGameId(), centerDTO, testUserCreator);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Only the hunter can update the game center"));
    }

    @Test
    public void updateGameCenter_centerTooFarAway_Forbidden() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testGame.setCenterLatitude(47.0);
        testGame.setCenterLongitude(8.0);
        testGame.setRadius(100.0);

        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);


        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.01);
        centerDTO.setLongitude(8.01);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGameCenter(testGame.getGameId(), centerDTO, testUserJoiner);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("New center is out of current game area"));
    }

    @Test
    public void updateGameCenter_exactBoundaryRadius() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);
        testGame.setCenterLatitude(47.0);
        testGame.setCenterLongitude(8.0);
        testGame.setRadius(100.0);


        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);

        // roughly 100m away
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.0009);
        centerDTO.setLongitude(8.0);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);


        // when
        Game updatedGame = gameService.updateGameCenter(testGame.getGameId(), centerDTO, testUserJoiner);

        // then
        verify(gameRepository, times(1)).save(testGame);
        assertEquals(centerDTO.getLatitude(), updatedGame.getCenterLatitude());
        assertEquals(centerDTO.getLongitude(), updatedGame.getCenterLongitude());
    }

    @Test
    public void updateGameCenter_gameNotFound() {
        // given
        Long nonExistentGameId = 999L;
        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.0005);
        centerDTO.setLongitude(8.0005);

        given(gameRepository.findByGameId(nonExistentGameId)).willReturn(null);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGameCenter(nonExistentGameId, centerDTO, testUserJoiner);
        });

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        assertTrue(exception.getReason().contains("Game not found"));
    }

    @Test
    public void updateGameCenter_gameNotInGame_Forbidden() {
        // given
        testGame.setStatus(GameStatus.IN_LOBBY);

        GameCenterDTO centerDTO = new GameCenterDTO();
        centerDTO.setLatitude(47.0005);
        centerDTO.setLongitude(8.0005);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when/then
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> {
            gameService.updateGameCenter(testGame.getGameId(), centerDTO, testUserJoiner);
        });

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatus());
        assertTrue(exception.getReason().contains("Game must be in progress to update center"));
    }

    // --- updateGameCenter Tests ---
    @Test
    public void computeRankings_someHidersCaught() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // Hunter
        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);

        // Hider 1 (not found)
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);
        testPlayerCreator.setFoundTime(null);

        // Hider 2 (first found)
        Player hider2 = new Player();
        hider2.setPlayerId(12L);
        User user2 = new User();
        user2.setUserId(3L);
        user2.setStats(new HashMap<>());
        hider2.setUser(user2);
        hider2.setRole(PlayerRole.HIDER);
        hider2.setStatus(PlayerStatus.FOUND);
        hider2.setFoundTime(LocalDateTime.now().minusMinutes(5));
        testGame.addPlayer(hider2);

        // Hider 3 (later found)
        Player hider3 = new Player();
        hider3.setPlayerId(13L);
        User user3 = new User();
        user3.setUserId(4L);
        user3.setStats(new HashMap<>());
        hider3.setUser(user3);
        hider3.setRole(PlayerRole.HIDER);
        hider3.setStatus(PlayerStatus.FOUND);
        hider3.setFoundTime(LocalDateTime.now().minusMinutes(2));
        testGame.addPlayer(hider3);

        // Mock for updateStats (seperately tested)
        GameService spyGameService = spy(gameService);
        doNothing().when(spyGameService).updateStats(any(Game.class));
        // when
        Game result = gameService.computeRankings(testGame);

        // then
        // not found Hiders are ranked first
        assertEquals(1, testPlayerCreator.getRank());
        // found Hiders are ranked based on foundTime
        assertEquals(2, hider2.getRank());
        assertEquals(3, hider3.getRank());
        // Hunter has lowest rank because not found all Hiders
        assertEquals(4, hunter.getRank());
    }

    @Test
    public void computeRankings_allHidersCaught() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // Hunter
        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);

        // Hider 1 (first found)
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.FOUND);
        testPlayerCreator.setFoundTime(LocalDateTime.now().minusMinutes(10));

        // Hider 2 (last found)
        Player hider2 = new Player();
        hider2.setPlayerId(12L);
        User user2 = new User();
        user2.setUserId(3L);
        user2.setStats(new HashMap<>());
        hider2.setUser(user2);
        hider2.setRole(PlayerRole.HIDER);
        hider2.setStatus(PlayerStatus.FOUND);
        hider2.setFoundTime(LocalDateTime.now().minusMinutes(5));
        testGame.addPlayer(hider2);

        // Mock for updateStats
        GameService spyGameService = spy(gameService);
        doNothing().when(spyGameService).updateStats(any(Game.class));
        // when
        Game result = gameService.computeRankings(testGame);

        // then
        // Hunter is ranked first
        assertEquals(1, hunter.getRank());
        // Hiders ranked based on foundTime
        assertEquals(2, testPlayerCreator.getRank());
        assertEquals(3, hider2.getRank());
    }

    @Test
    public void computeRankings_noHidersCaught() {
        // given
        testGame.setStatus(GameStatus.IN_GAME);

        // Hunter
        Player hunter = new Player();
        hunter.setPlayerId(11L);
        hunter.setUser(testUserJoiner);
        hunter.setRole(PlayerRole.HUNTER);
        hunter.setStatus(PlayerStatus.HUNTING);
        testGame.addPlayer(hunter);

        // Hider 1 (not found)
        testPlayerCreator.setRole(PlayerRole.HIDER);
        testPlayerCreator.setStatus(PlayerStatus.HIDING);
        testPlayerCreator.setFoundTime(null);

        // Hider 2 (not found)
        Player hider2 = new Player();
        hider2.setPlayerId(12L);
        User user2 = new User();
        user2.setUserId(3L);
        user2.setStats(new HashMap<>());
        hider2.setUser(user2);
        hider2.setRole(PlayerRole.HIDER);
        hider2.setStatus(PlayerStatus.HIDING);
        hider2.setFoundTime(null);
        testGame.addPlayer(hider2);

        // Mock for updateStats
        GameService spyGameService = spy(gameService);
        doNothing().when(spyGameService).updateStats(any(Game.class));
        // when
        Game result = gameService.computeRankings(testGame);

        // then
        // all Hiders ranked first
        assertEquals(1, testPlayerCreator.getRank());
        assertEquals(1, hider2.getRank());
        // Hunter is ranked last
        assertEquals(2, hunter.getRank());
    }

    // --- updateStats Tests ---
    @Test
    public void updateStats_winnerAndLosersUpdated_success() {
        // given
        testGame.setStatus(GameStatus.FINISHED);

        testPlayerCreator.setRank(1);
        testUserCreator.setStats(new HashMap<>());
        testUserCreator.getStats().put("wins", "2");
        testUserCreator.getStats().put("gamesPlayed", "5");
        testUserCreator.getStats().put("points", "50");

        Player player2 = new Player();
        player2.setPlayerId(12L);
        User user2 = new User();
        user2.setUserId(3L);
        user2.setStats(new HashMap<>());
        user2.getStats().put("wins", "1");
        user2.getStats().put("gamesPlayed", "3");
        user2.getStats().put("points", "30");
        player2.setUser(user2);
        player2.setRank(2);
        testGame.addPlayer(player2);

        Player player3 = new Player();
        player3.setPlayerId(13L);
        User user3 = new User();
        user3.setUserId(4L);
        user3.setStats(new HashMap<>());
        user3.getStats().put("wins", "0");
        user3.getStats().put("gamesPlayed", "2");
        user3.getStats().put("points", "15");
        player3.setUser(user3);
        player3.setRank(3);
        testGame.addPlayer(player3);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when
        gameService.updateStats(testGame);

        // then
        assertEquals("3", testUserCreator.getStats().get("wins"));
        assertEquals("6", testUserCreator.getStats().get("gamesPlayed"));
        assertEquals("60", testUserCreator.getStats().get("points"));

        assertEquals("1", user2.getStats().get("wins"));
        assertEquals("4", user2.getStats().get("gamesPlayed"));
        assertEquals("35", user2.getStats().get("points"));

        assertEquals("0", user3.getStats().get("wins"));
        assertEquals("3", user3.getStats().get("gamesPlayed"));
        assertEquals("15", user3.getStats().get("points"));

        assertNull(testPlayerCreator.getUser());
        assertNull(player2.getUser());
        assertNull(player3.getUser());
    }

    @Test
    public void updateStats_newUserWithoutStats_success() {
        // given
        testGame.setStatus(GameStatus.FINISHED);


        testPlayerCreator.setRank(1);
        testUserCreator.setStats(new HashMap<>());

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when
        gameService.updateStats(testGame);

        // then
        assertEquals("1", testUserCreator.getStats().get("wins"));
        assertEquals("1", testUserCreator.getStats().get("gamesPlayed"));
        assertEquals("10", testUserCreator.getStats().get("points"));
        assertNull(testPlayerCreator.getUser());
    }

    @Test
    public void updateStats_usersDecoupled_success() {
        // given
        testGame.setStatus(GameStatus.FINISHED);

        // Spieler 1
        testPlayerCreator.setRank(1);
        testUserCreator.setStats(new HashMap<>());
        testUserCreator.getStats().put("wins", "0");
        testUserCreator.getStats().put("gamesPlayed", "0");
        testUserCreator.getStats().put("points", "0");

        // Spieler 2
        Player player2 = new Player();
        player2.setPlayerId(12L);
        User user2 = new User();
        user2.setUserId(3L);
        user2.setStats(new HashMap<>());
        player2.setUser(user2);
        player2.setRank(2);
        testGame.addPlayer(player2);

        given(gameRepository.findByGameId(testGame.getGameId())).willReturn(testGame);

        // when
        gameService.updateStats(testGame);

        // then
        for (Player player : testGame.getPlayers()) {
            assertNull(player.getUser());
        }
    }
}
