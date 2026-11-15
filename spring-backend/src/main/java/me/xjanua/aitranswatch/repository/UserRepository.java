package me.xjanua.aitranswatch.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import me.xjanua.aitranswatch.model.User;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByUsernameIgnoreCase(String username);
    Optional<User> findByUsername(String username);
}
