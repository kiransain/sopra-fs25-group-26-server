package ch.uzh.ifi.hase.soprafs24.entity;

import ch.uzh.ifi.hase.soprafs24.constant.UserStatus;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Internal User Representation
 * This class composes the internal representation of the user and defines how
 * the user is stored in the database.
 * Every variable will be mapped into a database field with the @Column
 * annotation
 * - nullable = false -> this cannot be left empty
 * - unique = true -> this value must be unqiue across the database -> composes
 * the primary key
 */
@Entity
@Table(name = "USER")
public class User implements Serializable {

  private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue
  private Long userId;

  @Column(nullable = false, unique = true)
  private String username;

  @Column(nullable = false, unique = true)
  private String token;

  // defines new table for stats with foreign_key user_id
  @ElementCollection
  @CollectionTable(name = "user_stats", joinColumns = @JoinColumn(name = "user_id"))
  @MapKeyColumn(name = "stat_key")
  @Column(name = "stat_value")
  private Map<String, String> stats = new HashMap<>();



  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
//use this and then stats.put to update values in map
    public Map<String, String> getStats() {
        return stats;
    }
//only needed if whole map is replaced by other
    public void setStats(Map<String, String> stats) {
        this.stats = stats;
    }
}
