package ch.uzh.ifi.hase.soprafs24.repository;

import ch.uzh.ifi.hase.soprafs24.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
public class UserRepositoryIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private UserRepository userRepository;

    @Test
    public void findByUsername_success() {
        // given
        // userId should be auto-generated
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setToken("token123");
        user.setProfilePicture("default.jpg");
        user.setStats(new HashMap<>());

        entityManager.persist(user);
        entityManager.flush();

        // when
        User found = userRepository.findByUsername(user.getUsername());

        // then
        assertNotNull(found.getUserId());
        assertEquals(user.getUsername(), found.getUsername());
        assertEquals(user.getPassword(), found.getPassword());
        assertEquals(user.getToken(), found.getToken());
        assertEquals(user.getProfilePicture(), found.getProfilePicture());
        assertTrue(found.getStats().isEmpty());
    }

    @Test
    public void findByToken_success() {
        // given
        User user = new User();
        user.setUsername("testuser");
        user.setPassword("password123");
        user.setToken("uniquetoken");
        user.setProfilePicture("avatar.png");

        entityManager.persist(user);
        entityManager.flush();

        // when
        User found = userRepository.findByToken(user.getToken());

        // then
        assertNotNull(found);
        assertEquals(user.getUsername(), found.getUsername());
        assertEquals(user.getToken(), found.getToken());
        assertEquals(user.getProfilePicture(), found.getProfilePicture());
    }

    @Test
    public void saveUser_success() {
        // given
        User user = new User();
        user.setUsername("newuser");
        user.setPassword("secure123");
        user.setToken("newtoken");
        user.setProfilePicture("profile.jpg");

        // when
        User savedUser = userRepository.save(user);
        entityManager.flush();

        // then
        assertNotNull(savedUser.getUserId());
        User found = entityManager.find(User.class, savedUser.getUserId());
        assertEquals(user.getUsername(), found.getUsername());
        assertEquals(user.getProfilePicture(), found.getProfilePicture());
    }

    @Test
    public void findAllUsers_success() {
        // given
        User user1 = new User();
        user1.setUsername("user1");
        user1.setPassword("pass1");
        user1.setToken("token1");
        user1.setProfilePicture("pic1.jpg");

        User user2 = new User();
        user2.setUsername("user2");
        user2.setPassword("pass2");
        user2.setToken("token2");
        user2.setProfilePicture("pic2.jpg");

        entityManager.persist(user1);
        entityManager.persist(user2);
        entityManager.flush();

        // when
        List<User> users = userRepository.findAll();

        // then
        assertEquals(2, users.size());
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user1")));
        assertTrue(users.stream().anyMatch(u -> u.getUsername().equals("user2")));
    }
}