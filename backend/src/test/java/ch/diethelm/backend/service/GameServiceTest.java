package ch.diethelm.backend.service;

import ch.diethelm.backend.model.Game;
import ch.diethelm.backend.repository.GameRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @InjectMocks
    private GameService gameService;

    @Test
    void getAllGames_returnsAllGamesFromRepository() {
        Game game1 = Game.builder()
                .id(1L)
                .title("The Witcher 3")
                .releaseDate(LocalDate.of(2015, 5, 19))
                .build();
        Game game2 = Game.builder()
                .id(2L)
                .title("Elden Ring")
                .releaseDate(LocalDate.of(2022, 2, 25))
                .build();
        when(gameRepository.findAll()).thenReturn(List.of(game1, game2));

        List<Game> result = gameService.getAllGames();

        assertEquals(2, result.size());
        assertEquals("The Witcher 3", result.get(0).getTitle());
        verify(gameRepository).findAll();
    }

    @Test
    void getGameById_existingId_returnsGame() {
        Game game = Game.builder()
                .id(1L)
                .title("Zelda")
                .releaseDate(LocalDate.of(2017, 3, 3))
                .build();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));

        Game result = gameService.getGameById(1L);

        assertEquals(1L, result.getId());
        assertEquals("Zelda", result.getTitle());
        verify(gameRepository).findById(1L);
    }

    @Test
    void getGameById_unknownId_throwsNoSuchElementException() {
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> gameService.getGameById(99L));

        verify(gameRepository).findById(99L);
    }

    @Test
    void createGame_savesAndReturnsGame() {
        Game input = Game.builder()
                .title("Hades")
                .description("Roguelike")
                .imageUrl("https://example.com/hades.jpg")
                .releaseDate(LocalDate.of(2020, 9, 17))
                .build();
        Game saved = Game.builder()
                .id(10L)
                .title("Hades")
                .description("Roguelike")
                .imageUrl("https://example.com/hades.jpg")
                .releaseDate(LocalDate.of(2020, 9, 17))
                .build();
        when(gameRepository.save(input)).thenReturn(saved);

        Game result = gameService.createGame(input);

        assertEquals(10L, result.getId());
        assertEquals("Hades", result.getTitle());
        verify(gameRepository).save(input);
    }

    @Test
    void updateGame_existingId_updatesAllFieldsAndSaves() {
        Game existing = Game.builder()
                .id(1L)
                .title("Alt")
                .description("Alte Beschreibung")
                .imageUrl("https://example.com/old.jpg")
                .releaseDate(LocalDate.of(2018, 1, 1))
                .build();
        Game updatedData = Game.builder()
                .title("Neu")
                .description("Neue Beschreibung")
                .imageUrl("https://example.com/new.jpg")
                .releaseDate(LocalDate.of(2020, 5, 5))
                .build();
        when(gameRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(gameRepository.save(any(Game.class))).thenAnswer(inv -> inv.getArgument(0));

        Game result = gameService.updateGame(1L, updatedData);

        assertEquals("Neu", result.getTitle());
        assertEquals("Neue Beschreibung", result.getDescription());
        assertEquals("https://example.com/new.jpg", result.getImageUrl());
        assertEquals(LocalDate.of(2020, 5, 5), result.getReleaseDate());
        verify(gameRepository).findById(1L);
        verify(gameRepository).save(existing);
    }

    @Test
    void updateGame_unknownId_throwsNoSuchElementException() {
        Game updatedData = Game.builder()
                .title("Neu")
                .releaseDate(LocalDate.of(2020, 5, 5))
                .build();
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> gameService.updateGame(99L, updatedData));

        verify(gameRepository).findById(99L);
        verify(gameRepository, never()).save(any());
    }

    @Test
    void deleteGame_existingId_deletesGame() {
        when(gameRepository.existsById(1L)).thenReturn(true);

        gameService.deleteGame(1L);

        verify(gameRepository).existsById(1L);
        verify(gameRepository).deleteById(1L);
    }

    @Test
    void deleteGame_unknownId_throwsNoSuchElementException() {
        when(gameRepository.existsById(99L)).thenReturn(false);

        assertThrows(NoSuchElementException.class, () -> gameService.deleteGame(99L));

        verify(gameRepository).existsById(99L);
        verify(gameRepository, never()).deleteById(any());
    }

    @Test
    void searchByTitle_delegatesToRepository() {
        Game game = Game.builder()
                .id(1L)
                .title("Hollow Knight")
                .releaseDate(LocalDate.of(2017, 2, 24))
                .build();
        when(gameRepository.findByTitleContainingIgnoreCase("hollow")).thenReturn(List.of(game));

        List<Game> result = gameService.searchByTitle("hollow");

        assertEquals(1, result.size());
        assertEquals("Hollow Knight", result.get(0).getTitle());
        verify(gameRepository).findByTitleContainingIgnoreCase("hollow");
    }
}
