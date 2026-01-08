package com.example.demo.service;

import com.example.demo.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {

    private final List<User> inMemoryUsers = new ArrayList<>();

    public UserService() {
        // Initialize with sample data
        inMemoryUsers.add(new User("1", "alice", "alice@example.com", "ADMIN", true));
        inMemoryUsers.add(new User("2", "bob", "bob@example.com", "USER", true));
        inMemoryUsers.add(new User("3", "charlie", "charlie@example.com", "USER", false));
        inMemoryUsers.add(new User("4", "diana", "diana@example.com", "MODERATOR", true));
        log.info("Initialized UserService with {} users", inMemoryUsers.size());
    }

    @Cacheable("users")
    public Optional<User> getUserById(String id) {
        log.info("Fetching user from database (cache miss): {}", id);
        return inMemoryUsers.stream()
                .filter(u -> u.getId().equals(id))
                .findFirst();
    }

    @Cacheable("users")
    public List<User> getAllUsers() {
        log.info("Fetching all users from database (cache miss)");
        return new ArrayList<>(inMemoryUsers);
    }

    public List<User> getInMemoryUsers() {
        return inMemoryUsers;
    }
}
