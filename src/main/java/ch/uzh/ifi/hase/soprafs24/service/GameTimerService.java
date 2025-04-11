package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Scheduler that changes game status to in game after 10minutes in game preparation
@Component
public class GameTimerService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Autowired
    private GameRepository gameRepository;

    public void startPreparationTimer(Long gameId) {
        scheduler.schedule(() -> {
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game != null && game.getStatus() == GameStatus.IN_GAME_PREPARATION) {
                game.setStatus(GameStatus.IN_GAME);
                game.setTimer(LocalDateTime.now());
                gameRepository.save(game);
                gameRepository.flush();
                System.out.println("Game " + gameId + " has started.");
            }
        }, 2, TimeUnit.MINUTES);
    }
}
