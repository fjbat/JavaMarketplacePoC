package com.dtme.marketplace.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dtme.marketplace.entities.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {

}
