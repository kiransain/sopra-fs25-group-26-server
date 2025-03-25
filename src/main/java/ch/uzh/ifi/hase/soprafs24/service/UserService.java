package ch.uzh.ifi.hase.soprafs24.service;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;
import ch.uzh.ifi.hase.soprafs24.entity.User;
import ch.uzh.ifi.hase.soprafs24.repository.UserRepository;
import ch.uzh.ifi.hase.soprafs24.rest.dto.EditPutDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LoginPostDTO;
import ch.uzh.ifi.hase.soprafs24.rest.dto.LogoutPutDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Date;
import java.util.List;
import java.util.UUID;

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
        newUser.setStatus(UserStatus.OFFLINE);
        newUser.setCreationDate(new Date().toString());
        checkIfUserExists(newUser);
        // saves the given entity but data is only persisted in the database once
        // flush() is called
        newUser = userRepository.save(newUser);
        userRepository.flush();

        log.debug("Created Information for User: {}", newUser);

        return newUser;
    }

    @Transactional
    public User logInUser(LoginPostDTO loginPostDTO) {
        User user = userRepository.findByName(loginPostDTO.getUsername());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        if (user.getPassword().equals(loginPostDTO.getPassword())) {
            user.setStatus(UserStatus.ONLINE);
            userRepository.save(user);
            userRepository.flush();
            return user;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
    }

    public boolean logOutUser(LogoutPutDTO logoutPutDTO) {
        User user = userRepository.findByToken(logoutPutDTO.getToken());
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token");
        }
        if (user.getToken().equals(logoutPutDTO.getToken())) {
            user.setStatus(UserStatus.OFFLINE);
            userRepository.save(user);
            userRepository.flush();
            return true;
        } else {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid token");
        }
    }

    public boolean authenticateUser(String token) {
        User user = userRepository.findByToken(token);
        if (user != null && user.getStatus() == UserStatus.ONLINE) {
            return true;
        } else {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authorization failed");
        }
    }

    public User getUserById(long id) {
        List<User> users = getUsers();
        for (User user : users) {
            if (user.getId() == id) {
                return (user);
            }
        }  throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
    }

    public void update(long id, EditPutDTO editPutDTO) {
        User user = getUserById(id);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        user.setUsername(editPutDTO.getUsername());
        if (editPutDTO.getBirthday() != null) {
            user.setBirthday(editPutDTO.getBirthday());
        }
        userRepository.save(user);
        userRepository.flush();

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
