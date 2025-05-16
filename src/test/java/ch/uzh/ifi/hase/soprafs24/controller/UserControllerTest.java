package ch.uzh.ifi.hase.soprafs24.controller;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPutDTO;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * UserControllerTest
 * Class tests REST Endpoints for the UserController
 */
@WebMvcTest(UserController.class)
public class UserControllerTest {

    private final String VALID_TOKEN = "Bearer valid-token";
    private final String INVALID_TOKEN = "Bearer invalid-token";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private UserService userService;
    @MockBean
    private UserRepository userRepository;

    /**
     * Generate Test User
     */
    private User createTestUser() {
        User user = new User();
        user.setUserId(1L);
        user.setUsername("testUser");
        user.setPassword("password");
        user.setToken("valid-token");
        user.setProfilePicture("https://example.com/profile.jpg");
        Map<String, String> stats = new HashMap<>();
        stats.put("wins", "5");
        stats.put("gamesPlayed", "10");
        stats.put("points", "50");
        user.setStats(stats);
        return user;
    }

    /**
     * Converts Object to JSON String
     */
    private String asJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, String.format("The request body could not be created.%s", e.toString()));
        }
    }

    // GET /users
    @Test
    public void givenUsers_whenGetUsers_thenReturnJsonArray() throws Exception {
        // given
        User user = createTestUser();
        List<User> allUsers = Collections.singletonList(user);

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(user);
        given(userService.getUsers()).willReturn(allUsers);

        // when/then
        MockHttpServletRequestBuilder getRequest = get("/users")
                .header("Authorization", VALID_TOKEN)
                .contentType(MediaType.APPLICATION_JSON);

        mockMvc.perform(getRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$[0].username", is(user.getUsername())))
                .andExpect(jsonPath("$[0].token", is(user.getToken())))
                .andExpect(jsonPath("$[0].profilePicture", is(user.getProfilePicture())))
                .andExpect(jsonPath("$[0].stats.wins", is(user.getStats().get("wins"))))
                .andExpect(jsonPath("$[0].stats.gamesPlayed", is(user.getStats().get("gamesPlayed"))))
                .andExpect(jsonPath("$[0].stats.points", is(user.getStats().get("points"))));
    }

    @Test
    public void givenInvalidToken_whenGetUsers_thenReturn401() throws Exception {
        // given
        given(userService.authenticateUser(INVALID_TOKEN))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(get("/users")
                        .header("Authorization", INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    // POST /users
    @Test
    public void createUser_validInput_userCreated() throws Exception {
        // given
        User user = createTestUser();
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password");

        given(userService.createUser(Mockito.any())).willReturn(user);

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void createUser_duplicateUsername_throwsConflict() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password");

        given(userService.createUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isConflict());
    }

    // POST /login
    @Test
    public void loginUser_validCredentials_userLoggedIn() throws Exception {
        // given
        User user = createTestUser();
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("password");

        given(userService.loginUser(Mockito.any())).willReturn(user);

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
    public void loginUser_invalidCredentials_throwsUnauthorized() throws Exception {
        // given
        UserPostDTO userPostDTO = new UserPostDTO();
        userPostDTO.setUsername("testUser");
        userPostDTO.setPassword("wrongPassword");

        given(userService.loginUser(Mockito.any()))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

        // when/then
        MockHttpServletRequestBuilder postRequest = post("/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(asJsonString(userPostDTO));

        mockMvc.perform(postRequest)
                .andExpect(status().isUnauthorized());
    }

    // GET /users/{userId}
    @Test
    public void getUser_validUserAndAuthenticated_returnsUser() throws Exception {
        // given
        User user = createTestUser();

        given(userService.getOwnProfile(VALID_TOKEN, 1L)).willReturn(user);

        // when/then
        mockMvc.perform(get("/users/1")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void getUser_unauthorized_returns401() throws Exception {
        // given
        given(userService.getOwnProfile(INVALID_TOKEN, 1L))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(get("/users/1")
                        .header("Authorization", INVALID_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void getUser_userNotFound_returns404() throws Exception {
        // given
        User authUser = createTestUser();
        given(userService.authenticateUser(VALID_TOKEN)).willReturn(authUser);
        given(userService.getOwnProfile(VALID_TOKEN, 999L))
                .willThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));

        // when/then
        mockMvc.perform(get("/users/999")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isNotFound());
    }

    // GET /me
    @Test
    public void getCurrentUser_validToken_returnsCurrentUser() throws Exception {
        // given
        User user = createTestUser();

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(user);

        // when/then
        mockMvc.perform(get("/me")
                        .header("Authorization", VALID_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId", is(user.getUserId().intValue())))
                .andExpect(jsonPath("$.username", is(user.getUsername())))
                .andExpect(jsonPath("$.token", is(user.getToken())));
    }

    @Test
    public void getCurrentUser_invalidToken_returns401() throws Exception {
        // given
        given(userService.authenticateUser(INVALID_TOKEN))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(get("/me")
                        .header("Authorization", INVALID_TOKEN))
                .andExpect(status().isUnauthorized());
    }

    // PUT /users/{userId}
    @Test
    public void updateUser_validUpdate_success() throws Exception {
        // given
        User user = createTestUser();
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setPassword("newPassword");
        userPutDTO.setProfilePicture("https://example.com/new-profile.jpg");

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(user);
        doNothing().when(userService).updateUser(eq(1L), any(UserPutDTO.class), eq(user));

        // when/then
        mockMvc.perform(put("/users/1")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPutDTO)))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).updateUser(eq(1L), any(UserPutDTO.class), eq(user));
    }

    @Test
    public void updateUser_invalidToken_returns401() throws Exception {
        // given
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setPassword("newPassword");
        userPutDTO.setProfilePicture("https://example.com/new-profile.jpg");

        given(userService.authenticateUser(INVALID_TOKEN))
                .willThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));

        // when/then
        mockMvc.perform(put("/users/1")
                        .header("Authorization", INVALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPutDTO)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void updateUser_differentUser_returnsForbidden() throws Exception {
        // given
        User user = createTestUser();
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setPassword("newPassword");

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Cannot update other users"))
                .when(userService).updateUser(eq(2L), any(UserPutDTO.class), eq(user));

        // when/then
        mockMvc.perform(put("/users/2")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPutDTO)))
                .andExpect(status().isForbidden());
    }

    @Test
    public void updateUser_userNotFound_returns404() throws Exception {
        // given
        User user = createTestUser();
        UserPutDTO userPutDTO = new UserPutDTO();
        userPutDTO.setPassword("newPassword");

        given(userService.authenticateUser(VALID_TOKEN)).willReturn(user);
        doThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))
                .when(userService).updateUser(eq(999L), any(UserPutDTO.class), eq(user));

        // when/then
        mockMvc.perform(put("/users/999")
                        .header("Authorization", VALID_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJsonString(userPutDTO)))
                .andExpect(status().isNotFound());
    }
}