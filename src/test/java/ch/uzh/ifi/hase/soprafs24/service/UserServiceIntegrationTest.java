package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@WebAppConfiguration
@SpringBootTest
public class UserServiceIntegrationTest {

    @Qualifier("userRepository")
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    @BeforeEach
    public void setup() {
        userRepository.deleteAll();
    }

    @Test
    public void integrationTest_registerLoginUpdatePassword_success() {
        // register a new user
        User newUser = new User();
        newUser.setUsername("testuser");
        newUser.setPassword("initialPassword");

        User createdUser = userService.createUser(newUser);

        // check registration
        assertNotNull(createdUser.getUserId());
        assertNotNull(createdUser.getToken());
        assertEquals("testuser", createdUser.getUsername());
        assertEquals("initialPassword", createdUser.getPassword());
        assertEquals("0", createdUser.getStats().get("gamesPlayed"));

        // login user
        UserPostDTO loginDTO = new UserPostDTO();
        loginDTO.setUsername("testuser");
        loginDTO.setPassword("initialPassword");

        User loggedInUser = userService.loginUser(loginDTO);

        // check login
        assertEquals(createdUser.getUserId(), loggedInUser.getUserId());
        assertEquals(createdUser.getToken(), loggedInUser.getToken());

        // update password
        UserPutDTO updateDTO = new UserPutDTO();
        updateDTO.setPassword("newSecurePassword");

        userService.updateUser(loggedInUser.getUserId(), updateDTO, loggedInUser);

        // check password update
        User updatedUser = userRepository.findByUsername("testuser");
        assertEquals("newSecurePassword", updatedUser.getPassword());

        // login with new password
        UserPostDTO newLoginDTO = new UserPostDTO();
        newLoginDTO.setUsername("testuser");
        newLoginDTO.setPassword("newSecurePassword");

        User newLoggedInUser = userService.loginUser(newLoginDTO);

        // check login with new password
        assertEquals(createdUser.getUserId(), newLoggedInUser.getUserId());
    }
}