package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.entity.Player;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository("playerRepository")
public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByGame(Game game);

    Optional<Player> findByUser(User user); // one player per user

    boolean existsByUser(User user);// check if user is already a player

    Player findPlayerByPlayerId(long playerId);
}