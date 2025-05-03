package ch.uzh.ifi.hase.soprafs24.rest.dto;

public class UserPutDTO {

    private String password;
    private String profilePicture;

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getProfilePicture() {
        return profilePicture;
    }

    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
}
