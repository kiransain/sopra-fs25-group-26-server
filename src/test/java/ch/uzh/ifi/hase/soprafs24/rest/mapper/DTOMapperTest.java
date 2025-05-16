package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerRole;
import ch.uzh.ifi.hase.soprafs24.constant.PlayerStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DTOMapperTest
 * Tests if the mapping between the internal and the external/API representation
 * works.
 */
public class DTOMapperTest {

    @Test
    public void testCreateUser_fromUserPostDTO_toEntity() {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword");
        userPostDTO.setProfilePicture("testPicture");

        // when
        User user = DTOMapper.INSTANCE.convertUserPostDTOtoEntity(userPostDTO);

        // then
        assertEquals(userPostDTO.getUsername(), user.getUsername());
        assertEquals(userPostDTO.getPassword(), user.getPassword());
        assertEquals(userPostDTO.getProfilePicture(), user.getProfilePicture());
        assertNull(user.getUserId());
        assertNull(user.getToken());
        assertTrue(user.getStats().isEmpty());
    }

    @Test
    public void testGetUser_fromEntity_toUserGetDTO() {
        // given
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testUsername");
        user.setToken("testToken");
        user.setProfilePicture("testPicture");
        Map<String, String> stats = new HashMap<>();
        stats.put("wins", "5");
        stats.put("gamesPlayed", "10");
        user.setStats(stats);

        // when
        UserGetDTO userGetDTO = DTOMapper.INSTANCE.convertEntityToUserGetDTO(user);

        // then
        assertEquals(user.getUserId(), userGetDTO.getUserId());
        assertEquals(user.getUsername(), userGetDTO.getUsername());
        assertEquals(user.getToken(), userGetDTO.getToken());
        assertEquals(user.getProfilePicture(), userGetDTO.getProfilePicture());
        assertEquals(user.getStats(), userGetDTO.getStats());
    }

    @Test
    public void testGetGame_fromEntity_toGameGetDTO() {
        // given
        Game game = new Game();
        game.setGameId(1L);
        game.setGamename("testGame");
        game.setStatus(GameStatus.IN_LOBBY);
        game.setCenterLatitude(47.4);
        game.setCenterLongitude(8.5);
        game.setRadius(100.0);
        game.setPreparationTimeInSeconds(30);
        game.setGameTimeInSeconds(300);
        game.setTimer(LocalDateTime.now());

        // Create creator
        Player creator = new Player();
        creator.setPlayerId(1L);
        game.setCreator(creator);

        // Create players
        Player player1 = new Player();
        player1.setPlayerId(1L);
        Player player2 = new Player();
        player2.setPlayerId(2L);
        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);
        game.setPlayers(players);

        // when
        GameGetDTO gameGetDTO = DTOMapper.INSTANCE.convertEntityToGameGetDTO(game);

        // then
        assertEquals(game.getGameId(), gameGetDTO.getGameId());
        assertEquals(game.getGamename(), gameGetDTO.getGamename());
        assertEquals(game.getStatus(), gameGetDTO.getStatus());
        assertEquals(game.getCenterLatitude(), gameGetDTO.getCenterLatitude());
        assertEquals(game.getCenterLongitude(), gameGetDTO.getCenterLongitude());
        assertEquals(game.getRadius(), gameGetDTO.getRadius());
        assertEquals(game.getPreparationTimeInSeconds(), gameGetDTO.getPreparationTimeInSeconds());
        assertEquals(game.getGameTimeInSeconds(), gameGetDTO.getGameTimeInSeconds());
        assertEquals(game.getTimer(), gameGetDTO.getTimer());
        assertEquals(game.getCreator().getPlayerId(), gameGetDTO.getCreatorId());
        assertEquals(2, gameGetDTO.getPlayers().size());
    }

    @Test
    public void testGetGames_fromEntities_toGameGetDTOs() {
        // given
        Game game1 = new Game();
        game1.setGameId(1L);
        game1.setGamename("Game 1");
        game1.setCreator(new Player());
        game1.getCreator().setPlayerId(1L);
        game1.setPlayers(new ArrayList<>());

        Game game2 = new Game();
        game2.setGameId(2L);
        game2.setGamename("Game 2");
        game2.setCreator(new Player());
        game2.getCreator().setPlayerId(2L);
        game2.setPlayers(new ArrayList<>());

        List<Game> games = new ArrayList<>();
        games.add(game1);
        games.add(game2);

        // when
        List<GameGetDTO> gameGetDTOs = DTOMapper.INSTANCE.convertEntityToGameGetDTO(games);

        // then
        assertEquals(2, gameGetDTOs.size());
        assertEquals(game1.getGameId(), gameGetDTOs.get(0).getGameId());
        assertEquals(game1.getGamename(), gameGetDTOs.get(0).getGamename());
        assertEquals(game2.getGameId(), gameGetDTOs.get(1).getGameId());
        assertEquals(game2.getGamename(), gameGetDTOs.get(1).getGamename());
    }

    @Test
    public void testGetPlayer_fromEntity_toPlayerGetDTO() {
        // given
        Player player = new Player();
        player.setPlayerId(1L);
        player.setDisplayName("TestPlayer");
        player.setDisplayPicture("TestPicture");
        player.setRole(PlayerRole.HIDER);
        player.setStatus(PlayerStatus.HIDING);
        player.setOutOfArea(false);
        player.setFoundTime(LocalDateTime.now());
        player.setLocationLat(47.4);
        player.setLocationLong(8.5);
        player.setRank(2);

        User user = new User();
        user.setUserId(10L);
        player.setUser(user);

        // when
        PlayerGetDTO playerGetDTO = DTOMapper.INSTANCE.convertEntityToPlayerGetDTO(player);

        // then
        assertEquals(player.getPlayerId(), playerGetDTO.getPlayerId());
        assertEquals(player.getUser().getUserId(), playerGetDTO.getUserId());
        assertEquals(player.getDisplayName(), playerGetDTO.getDisplayName());
        assertEquals(player.getDisplayPicture(), playerGetDTO.getDisplayPicture());
        assertEquals(player.getRole(), playerGetDTO.getRole());
        assertEquals(player.getStatus(), playerGetDTO.getStatus());
        assertEquals(player.isOutOfArea(), playerGetDTO.isOutOfArea());
        assertEquals(player.getFoundTime(), playerGetDTO.getFoundTime());
        assertEquals(player.getLocationLat(), playerGetDTO.getLocationLat());
        assertEquals(player.getLocationLong(), playerGetDTO.getLocationLong());
        assertEquals(player.getRank(), playerGetDTO.getRank());
    }

    @Test
    public void testGetPlayers_fromEntities_toPlayerGetDTOs() {
        // given
        Player player1 = new Player();
        player1.setPlayerId(1L);
        player1.setDisplayName("Player 1");
        player1.setUser(new User());
        player1.getUser().setUserId(1L);

        Player player2 = new Player();
        player2.setPlayerId(2L);
        player2.setDisplayName("Player 2");
        player2.setUser(new User());
        player2.getUser().setUserId(2L);

        List<Player> players = new ArrayList<>();
        players.add(player1);
        players.add(player2);

        // when
        List<PlayerGetDTO> playerGetDTOs = DTOMapper.INSTANCE.convertEntityToPlayerGetDTO(players);

        // then
        assertEquals(2, playerGetDTOs.size());
        assertEquals(player1.getPlayerId(), playerGetDTOs.get(0).getPlayerId());
        assertEquals(player1.getDisplayName(), playerGetDTOs.get(0).getDisplayName());
        assertEquals(player2.getPlayerId(), playerGetDTOs.get(1).getPlayerId());
        assertEquals(player2.getDisplayName(), playerGetDTOs.get(1).getDisplayName());
    }
}