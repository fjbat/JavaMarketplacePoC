package com.dtme.marketplace.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dtme.marketplace.entities.AuthenticationMethod;

public interface AuthenticationMethodRepository extends JpaRepository<AuthenticationMethod, Long> {
}

