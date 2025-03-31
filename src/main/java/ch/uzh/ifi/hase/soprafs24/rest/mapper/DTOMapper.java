package ch.uzh.ifi.hase.soprafs24.rest.mapper;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserGetDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GamePostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.PlayerInfoDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.GameGetDTO;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

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
    User convertUserPostDTOtoEntity(UserPostDTO userPostDTO);

    @Mapping(source = "userId", target = "userId")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "token", target = "token")
    @Mapping(source = "stats", target = "stats")
    UserGetDTO convertEntityToUserGetDTO(User user);


    @Mapping(source = "gameName", target = "gameName")
    @Mapping(target = "gameId", ignore = true) // Auto-generated
    @Mapping(target = "creationDate", ignore = true) // Set in service
    @Mapping(target = "centerLatitude", ignore = true) // Set later
    @Mapping(target = "centerLongitude", ignore = true) // Set later
    @Mapping(target = "radius", ignore = true) // Set later
    @Mapping(target = "status", ignore = true) // Set later
    @Mapping(target = "players", ignore = true) // The player that created the game will be added in service
    Game convertGamePostDTOtoEntity(GamePostDTO gamePostDTO);

    @Mapping(source = "gameId", target = "gameId")
    @Mapping(source = "gameName", target = "gameName")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "creationDate", target = "creationDate")
    @Mapping(source = "centerLatitude", target = "centerLatitude")
    @Mapping(source = "centerLongitude", target = "centerLongitude")
    @Mapping(source = "radius", target = "radius")
    @Mapping(source = "players", target = "players", qualifiedByName = "playerListToPlayerInfoDTOList") // Map players list
    GameGetDTO convertEntityToGameGetDTO(Game game);

    //needed to convert List<Player> to List<PlayerInfoDTO> in the GameGetDTO mapper
    @Named("playerListToPlayerInfoDTOList")
    List<PlayerInfoDTO> convertEntityListToPlayerInfoDTOList(List<Player> players);


    // this will be needed for the implementation of get games
    List<GameGetDTO> convertEntityListToGameGetDTOList(List<Game> games);


    @Mapping(source = "playerId", target = "playerId")
    @Mapping(source = "user.userId", target = "userId")
    @Mapping(source = "user.username", target = "username")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "status", target = "status")
    PlayerInfoDTO convertEntityToPlayerInfoDTO(Player player);
}
