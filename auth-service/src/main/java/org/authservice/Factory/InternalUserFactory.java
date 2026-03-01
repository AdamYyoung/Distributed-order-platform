package org.authservice.Factory;

import org.authservice.DAO.User;
import org.springframework.stereotype.Component;

@Component
public class InternalUserFactory implements UserFactory {

    @Override
    public User createrUser(String username, String encodePassword) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(encodePassword);
        return user;
    }
}
