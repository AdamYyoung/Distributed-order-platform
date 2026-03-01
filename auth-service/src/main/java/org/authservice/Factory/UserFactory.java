package org.authservice.Factory;

import org.authservice.DAO.User;

public interface UserFactory {
    User createrUser(String username, String password);
}
