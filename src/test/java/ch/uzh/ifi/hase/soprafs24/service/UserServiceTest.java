package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {


    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUserId(1L);
        testUser.setUsername("Test_User");
        testUser.setPassword("Test_Password");
        testUser.setToken("Test_Token");

    }

    @Test
    void createUser_validInput_assignsAllFields() {

        User newUser = new User();
        newUser.setUsername("New_User");
        newUser.setPassword("New_Password");

        when(userRepository.findByUsername("New_User")).thenReturn(null);
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User saved = invocation.getArgument(0);
                    saved.setUserId(1L);
                    return saved;
                });


        User createdUser = userService.createUser(newUser);

        assertNotNull(createdUser.getUserId());
        assertTrue(createdUser.getProfilePicture().startsWith("https://ui-avatars.com/api/?name="));
        assertEquals(createdUser.getUsername(), newUser.getUsername());
        assertNotNull(createdUser.getToken());
        assertNotNull(createdUser.getStats().get("creation_date"));
        assertEquals("0", createdUser.getStats().get("gamesPlayed"));
        assertEquals("0", createdUser.getStats().get("wins"));
        assertEquals("0", createdUser.getStats().get("points"));
    }

    @Test
    void createUser_duplicateUsername_throwsConflict() {

        when(userRepository.findByUsername("Test_User")).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.createUser(testUser)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void authenticateUser_validToken_returnsUser() {

        String authorizationHeader = "Bearer " + testUser.getToken();

        when(userRepository.findByToken(testUser.getToken())).thenReturn(testUser);

        User authenticatedUser = userService.authenticateUser(authorizationHeader);

        assertSame(authenticatedUser, testUser); //check if exactly same object is returne
        verify(userRepository).findByToken(testUser.getToken());
    }

    @Test
    void authenticateUser_invalidToken_throwsUnauthorized() {

        String authorizationHeader = "Bearer Invalid_Token";

        when(userRepository.findByToken("Invalid_Token"))
                .thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.authenticateUser(authorizationHeader)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(userRepository).findByToken("Invalid_Token");
    }

    @Test
    void loginUser_validCredentials_returnsUser() {

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("Test_User");
        dto.setPassword("Test_Password");

        when(userRepository.findByUsername("Test_User")).thenReturn(testUser);

        User result = userService.loginUser(dto);

        assertSame(testUser, result);
        verify(userRepository).findByUsername("Test_User");

    }

    @Test
    void loginUser_invalidUsername_throwsUnauthorized() {

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("Invalid_Username");
        dto.setPassword("Test_Password");

        when(userRepository.findByUsername("Invalid_Username")).thenReturn(null);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser(dto)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(userRepository).findByUsername("Invalid_Username");
    }

    @Test
    void loginUser_invalidPassword_throwsUnauthorized() {

        UserPostDTO dto = new UserPostDTO();
        dto.setUsername("Test_User");
        dto.setPassword("Invalid_Password");

        when(userRepository.findByUsername("Test_User")).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.loginUser(dto)
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
        verify(userRepository).findByUsername("Test_User");
    }

    @Test
    void getOwnProfile_validUserId_returnsUser() {
        String authorizationHeader = "Bearer " + testUser.getToken();
        Long requestedUserId = testUser.getUserId();

        when(userRepository.findByToken(testUser.getToken())).thenReturn(testUser);

        User result = userService.getOwnProfile(authorizationHeader, requestedUserId);

        assertSame(testUser, result);
        verify(userRepository).findByToken(testUser.getToken());
    }

    @Test
    void getOwnProfile_invalidUserId_throwsUnauthorized() {
        String authorizationHeader = "Bearer " + testUser.getToken();
        Long requestedUserId = 999L;

        when(userRepository.findByToken(testUser.getToken())).thenReturn(testUser);

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.getOwnProfile(authorizationHeader, requestedUserId)
        );

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        verify(userRepository).findByToken(testUser.getToken());
    }

    @Test
    void updateUser_validInput_updatesUser() {

        UserPutDTO userPutDto = new UserPutDTO();
        userPutDto.setPassword("New_Password");

        Long requestedUserId = testUser.getUserId();

        when(userRepository.findById(requestedUserId)).thenReturn(Optional.ofNullable(testUser));

        userService.updateUser(requestedUserId, userPutDto, testUser);

        assertEquals("New_Password", testUser.getPassword());
    }

    @Test
    void updateUser_invalidUserId_throwsForbidden() {

        User otherUser = new User();
        otherUser.setUserId(999L);

        UserPutDTO userPutDto = new UserPutDTO();
        userPutDto.setPassword("New_Password");

        Long requestedUserId = otherUser.getUserId();

        when(userRepository.findById(requestedUserId)).thenReturn(Optional.of(otherUser));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(requestedUserId, userPutDto, testUser)
        );

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateUser_samePassword_throwsBadRequest() {

        UserPutDTO userPutDto = new UserPutDTO();
        userPutDto.setPassword(testUser.getPassword());

        Long requestedUserId = testUser.getUserId();

        when(userRepository.findById(requestedUserId)).thenReturn(Optional.ofNullable(testUser));

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(requestedUserId, userPutDto, testUser)
        );

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());

    }

    @Test
    void updateUser_notFound_throwsNotFound() {

        UserPutDTO dto = new UserPutDTO();
        dto.setPassword("Does_Not_Matter");

        Long requestedUserId = 999L;

        when(userRepository.findById(requestedUserId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> userService.updateUser(requestedUserId, dto, testUser)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());

        verify(userRepository).findById(requestedUserId);
        verify(userRepository, never()).save(any());
    }
}
