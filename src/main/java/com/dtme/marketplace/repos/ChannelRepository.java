package com.dtme.marketplace.repos;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dtme.marketplace.entities.Channel;

public interface ChannelRepository extends JpaRepository<Channel, Long> {

}
