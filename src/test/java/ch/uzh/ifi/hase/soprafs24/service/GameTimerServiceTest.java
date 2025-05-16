package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.GameStatus;
import ch.uzh.ifi.hase.soprafs24.entity.Game;
import ch.uzh.ifi.hase.soprafs24.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class GameTimerServiceTest {


    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameService gameService;

    @InjectMocks
    private GameTimerService gameTimerService;

    @Test
    void startPreparationTimer_changesGameStatus() throws Exception {
        // Given
        Game testGame = new Game();
        testGame.setGameId(1L);
        testGame.setStatus(GameStatus.IN_GAME_PREPARATION);

        // Mock repository behavior
        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));

        // When with small delay
        gameTimerService.startPreparationTimer(1L, 1, 1);
        Thread.sleep(1500);

        // Then
        verify(gameRepository).save(argThat(savedGame ->
                savedGame.getStatus() == GameStatus.IN_GAME));
        verify(gameRepository).flush();
    }

    @Test
    void startFinishTimer_callsFinishGame() throws Exception {
        // Given
        Game testGame = new Game();
        testGame.setGameId(1L);
        testGame.setStatus(GameStatus.IN_GAME);

        when(gameRepository.findById(1L)).thenReturn(Optional.of(testGame));

        // When with small delay
        gameTimerService.startFinishTimer(1L, 1);
        Thread.sleep(1500);

        // Then
        verify(gameService).finishGame(1L);
    }

    @Test
    void stopFinishTimer_cancelsScheduledTask() throws Exception {
        // Given
        gameTimerService.startFinishTimer(1L, 60);

        // When
        gameTimerService.stopFinishTimer(1L);

        // Then 
        Thread.sleep(100);
        verify(gameService, never()).finishGame(1L);
    }
}
