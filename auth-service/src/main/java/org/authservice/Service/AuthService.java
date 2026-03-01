package org.authservice.Service;

public interface AuthService {
    String register(String username, String password, String invitationCode);
    String login(String username, String password);
}
