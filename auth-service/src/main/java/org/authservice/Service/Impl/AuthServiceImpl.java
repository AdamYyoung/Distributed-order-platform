package org.authservice.Service.Impl;

import lombok.RequiredArgsConstructor;
import org.authservice.DAO.Role;
import org.authservice.DAO.User;
import org.authservice.Factory.InternalUserFactory;
import org.authservice.Repository.RoleRepo;
import org.authservice.Repository.UserRepo;
import org.authservice.Service.AuthService;
import org.commonlib.Utils.JwtUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final InternalUserFactory internalUserFactory;
    private final JwtUtils jwtUtils;

    @Override
    @Transactional
    public String register(String username, String password, String invitationCode) {
        // 1. Repeat username
        if (userRepo.existsByUsername(username)) {
            throw new RuntimeException("Username is already in use");
        }
        // 2. create
        User user = internalUserFactory.createrUser(username, passwordEncoder.encode(password));
        // 3. RBAC
        Set<Role> roles = new HashSet<>();
        roles.add(getOrCreateRole("ROLE_USER"));
        if ("isAdmin".equalsIgnoreCase(invitationCode)) {
            roles.add(getOrCreateRole("ROLE_ADMIN"));
        }
        user.setRoles(roles);
        userRepo.save(user);
        return "User registered successfully";
    }

    @Override
    @Transactional(readOnly = true)
    public String login(String username, String password) {
        // 1. check username
        User user = userRepo.findByUsername(username).orElseThrow(()->new RuntimeException("User not found"));
        System.out.println("0. not repeat");
        // 2. check password
        if (!passwordEncoder.matches(password, user.getPassword())){
            throw new RuntimeException("Incorrect password");
        }
        System.out.println("1. successful create");
        // 3. get roles from jwt
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        System.out.println("2. set roles!");
        return jwtUtils.generateToken(user.getId(), username, roles);
    }

    private Role getOrCreateRole(String name) {
        return roleRepo.findByName(name)
                .orElseGet(() -> roleRepo.save(new Role(null, name)));
    }
}
