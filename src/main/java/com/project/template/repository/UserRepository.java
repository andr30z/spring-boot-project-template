package com.project.template.repository;

import com.project.template.model.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
  @Query("select u from User u where u.email = ?1")
  Optional<User> findUserByEmail(String email);
}
