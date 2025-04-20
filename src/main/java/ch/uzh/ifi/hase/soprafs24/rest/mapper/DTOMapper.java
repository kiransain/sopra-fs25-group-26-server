package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * DTOMapper
 * This class is responsible for generating classes that will automatically
 * transform/map the internal representation
 * of an entity (e.g., the User) to the external/API representation (e.g.,
 * UserGetDTO for getting, UserPostDTO for creating)
 * and vice versa.
 * Additional mappers can be defined for new entities.
 * Always created one mapper for getting information (GET) and one mapper for
 * creating information (POST).
 */
@Mapper
public interface DTOMapper {

    DTOMapper INSTANCE = Mappers.getMapper(DTOMapper.class);

    @Mapping(source = "username", target = "username")
    @Mapping(source = "password", target = "password")
    @Mapping(target = "token", ignore = true)
    @Mapping(target = "stats", ignore = true)
    @Mapping(target = "userId", ignore = true)
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "stats", target = "stats")
    UserGetDTO convertEntityToUserGetDTO(User user);

    @Mapping(source = "gameId", target = "gameId")
    @Mapping(source = "gamename", target = "gamename")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "centerLatitude", target = "centerLatitude")
    @Mapping(source = "centerLongitude", target = "centerLongitude")
    @Mapping(source = "radius", target = "radius")
    @Mapping(source = "timer", target = "timer")
    @Mapping(source = "creator.playerId", target = "creatorId")
    @Mapping(source = "players", target = "players")
    GameGetDTO convertEntityToGameGetDTO(Game game);

    List<GameGetDTO> convertEntityToGameGetDTO(List<Game> games);

    @Mapping(source = "playerId", target = "playerId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "outOfArea", target = "outOfArea")
    @Mapping(source = "foundTime", target = "foundTime")
    @Mapping(source = "locationLat", target = "locationLat")
    @Mapping(source = "locationLong", target = "locationLong")
    @Mapping(source = "rank", target = "rank")
    PlayerGetDTO convertEntityToPlayerGetDTO(Player player);

    List<PlayerGetDTO> convertEntityToPlayerGetDTO(List<Player> players);
}
