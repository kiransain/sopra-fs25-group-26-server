package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.*;

// Scheduler that changes game status to in game in game preparation and also afterwards to finished
@Component
public class GameTimerService {

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    //hashmap that associates gameId with the scheduled future
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> finishTimers = new ConcurrentHashMap<>();

    @Autowired
    private GameRepository gameRepository;
    @Autowired
    private GameService gameService;

    public void startPreparationTimer(Long gameId) {
        scheduler.schedule(() -> {
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game != null && game.getStatus() == GameStatus.IN_GAME_PREPARATION) {
                game.setStatus(GameStatus.IN_GAME);
                game.setTimer(LocalDateTime.now());
                gameRepository.save(game);
                gameRepository.flush();
                System.out.println("Game " + gameId + " has started.");
                startFinishTimer(gameId);
                System.out.println("Finish timer has started");
            }
        }, 45, TimeUnit.SECONDS);
    }

    public void startFinishTimer(Long gameId) {
        ScheduledFuture<?> future = scheduler.schedule(() -> {
            System.out.println("Game " + gameId + " is about to finish.");
            Game game = gameRepository.findById(gameId).orElse(null);
            if (game != null && game.getStatus() == GameStatus.IN_GAME) {
                gameService.finishGame(gameId);
                System.out.println("Game " + gameId + " has finished.");
            }
        }, 60, TimeUnit.SECONDS);
        finishTimers.put(gameId, future);
    }

    // method called when game is finished because all hiders caught to stop timer
    public void stopFinishTimer(Long gameId) {
        ScheduledFuture<?> future = finishTimers.get(gameId);
        if (future != null) {
            future.cancel(false);
            finishTimers.remove(gameId);
            System.out.println("Finish timer for game " + gameId + " has been stopped.");
        }
    }
}
