package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test class for the User REST controller.
 *
 * @see UserController
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository; // Required for Controller context loading

    private User createTestUser(Long id, String username, String password, String token) {
        User user = new User();
        user.setUserId(id);
        user.setUsername(username);
        user.setPassword(password);
        user.setToken(token);
        Map<String, String> stats = new HashMap<>();
        stats.put("gamesPlayed", "5");
        stats.put("wins", "2");
        user.setStats(stats);
        return user;
    }

    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = createTestUser(1L, "testUsername", "testPassword", "token-1");
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword");

        given(userService.createUser(Mockito.any(User.class))).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())))
                .andExpect(jsonPath("$.stats.gamesPlayed", is(user.getStats().get("gamesPlayed"))))
                .andExpect(jsonPath("$.stats.wins", is(user.getStats().get("wins"))));
    }

    @Test
    public void createUser_usernameTaken_throwsConflict() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("existingUsername");
        userPostDTO.setPassword("password");

        given(userService.createUser(Mockito.any(User.class)))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    @Test
    public void loginUser_validCredentials_userLoggedIn() throws Exception {
        // given
        User user = createTestUser(1L, "testUsername", "testPassword", "token-1");
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("testPassword");

        given(userService.loginUser(Mockito.any(UserPostDTO.class))).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void loginUser_invalidUsername_throwsUnauthorized() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("nonExistentUser");
        userPostDTO.setPassword("password");

        given(userService.loginUser(Mockito.any(UserPostDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void loginUser_invalidPassword_throwsUnauthorized() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUsername");
        userPostDTO.setPassword("wrongPassword");

        given(userService.loginUser(Mockito.any(UserPostDTO.class)))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getAllUsers_success_returnsListOfUsers() throws Exception {
        // given
        User user = createTestUser(1L, "testUsername", "password", "token-1");
        List<User> allUsers = Collections.singletonList(user);

        given(userService.getUsers()).willReturn(allUsers);

        // when
        MockHttpServletRequestBuilder getRequest = get("/users")
                .contentType(MediaType.APPLICATION_JSON);

        // then
        mockMvc.perform(getRequest).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].token", is(user.getToken())));
    }

    @Test
    public void getUser_ownProfile_success() throws Exception {
        // given
        long userId = 1L;
        String validToken = "valid-token";
        User user = createTestUser(userId, "testUser", "password", validToken);

        given(userService.getOwnProfile(validToken, userId)).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", userId)
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void getUser_invalidToken_throwsUnauthorized() throws Exception {
        // given
        long userId = 1L;
        String invalidToken = "invalid-token";

        given(userService.getOwnProfile(invalidToken, userId))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization failed"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", userId)
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getUser_notOwnProfile_throwsNotFound() throws Exception {
        // given
        long requestedUserId = 2L;
        String validTokenForUser1 = "valid-token-user1";

        given(userService.getOwnProfile(validTokenForUser1, requestedUserId))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Not own profile"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users/{userId}", requestedUserId)
                .header("Authorization", validTokenForUser1)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isNotFound());
    }

    @Test
    public void getCurrentUser_validToken_returnsUser() throws Exception {
        // given
        String validToken = "valid-token";
        User user = createTestUser(1L, "currentUser", "password", validToken);

        given(userService.authenticateUser(validToken)).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/me")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void getCurrentUser_invalidToken_throwsUnauthorized() throws Exception {
        // given
        String invalidToken = "invalid-token";

        given(userService.authenticateUser(invalidToken))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization failed"));

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/me")
                .header("Authorization", invalidToken)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isUnauthorized());
    }


    /**
     * Helper Method to convert object into JSON string.
     * @param object the object to convert
     * @return JSON string
     */
    private String asJsonString(final Object object) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.findAndRegisterModules(); // Register JavaTimeModule etc.
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("The request body could not be created.%s", e.toString()));
        }
    }
}
