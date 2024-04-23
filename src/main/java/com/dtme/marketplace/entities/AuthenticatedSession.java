package com.dtme.marketplace.entities;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Index;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import org.apache.http.client.AuthenticationStrategy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;



/**
 * @description
 * An AuthenticatedSession is created upon successful authentication.
 *
 * @docsCategory entities
 */
@Entity
@Table(name = "authenticated_session")
@EntityListeners(AuditingEntityListener.class)
public class AuthenticatedSession extends Session {
    public AuthenticatedSession(DeepPartial<AuthenticatedSession> input) {
        super(input);
    }

    /**
     * @description
     * The {@link User} who has authenticated to create this session.
     */
    @Index
    @ManyToOne(targetEntity = User.class)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * @description
     * The name of the {@link AuthenticationStrategy} used when authenticating
     * to create this session.
     */
    @Column(name = "authentication_strategy")
    private String authenticationStrategy;
}

