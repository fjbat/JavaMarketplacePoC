package com.dtme.marketplace.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dtme.marketplace.entities.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
