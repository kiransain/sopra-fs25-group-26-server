package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.UserPostDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * User Service
 * This class is the "worker" and responsible for all functionality related to
 * the user
 * (e.g., it creates, modifies, deletes, finds). The result will be passed back
 * to the caller.
 */
@Service
@Transactional
public class UserService {

    private final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    @Autowired
    public UserService(@Qualifier("userRepository") UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return this.userRepository.findAll();
    }

    public User createUser(User newUser) {
        newUser.setToken(UUID.randomUUID().toString());

        Map<String, String> stats = new HashMap<>();
        stats.put("creation_date", new SimpleDateFormat("dd.MM.yyyy").format(new Date()));
        stats.put("games_played", "0");
        stats.put("games_won", "0");
        newUser.setStats(stats);

        checkIfUserExists(newUser);
        // saves the given entity but data is only persisted in the database once
        // flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);
        return newUser;
    }

    @Transactional
    public User loginUser(UserPostDTO userPostDTO) {
        User user = userRepository.findByUsername(userPostDTO.getUsername());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getPassword().equals(userPostDTO.getPassword())) {
            return user;
        }
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public User authenticateUser(String authorizationHeader) {
        String token = authorizationHeader.replace("Bearer ", "");
        User user = userRepository.findByToken(token);
        if (user != null) {
            return user;
        }
        else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization failed");
        }
    }

    public User getOwnProfile(String authorizationHeader, Long requestedUserId) {
        User user = authenticateUser(authorizationHeader);
        if (user.getUserId().equals(requestedUserId)) {
            return user;
        }
        else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Not own profile");
        }
    }

    /**
     * This is a helper method that will check the uniqueness criteria of the
     * username and the name
     * defined in the User entity. The method will do nothing if the input is unique
     * and throw an error otherwise.
     *
     * @param userToBeCreated
     * @throws org.springframework.web.server.ResponseStatusException
     * @see User
     */
    private void checkIfUserExists(User userToBeCreated) {
        User userByUsername = userRepository.findByUsername(userToBeCreated.getUsername());

        String baseErrorMessage = "The username provided is not unique. Therefore, the user could not be created!";
        if (userByUsername != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, baseErrorMessage);
        }
    }
}
