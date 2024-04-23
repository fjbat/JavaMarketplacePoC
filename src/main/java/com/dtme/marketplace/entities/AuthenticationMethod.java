package com.dtme.marketplace.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Map;

import javax.persistence.*;

/**
 * @description An AuthenticationMethod represents the means by which a
 *              {@link User} is authenticated. There are two kinds:
 *              {@link NativeAuthenticationMethod} and
 *              {@link ExternalAuthenticationMethod}.
 *
 * @docsCategory entities
 * @docsPage AuthenticationMethod
 */
@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "type", discriminatorType = DiscriminatorType.CHAR)
@EntityListeners(AuditingEntityListener.class)
public class AuthenticationMethod extends VendureEntity {

	public AuthenticationMethod() {
		super();

	}

	protected AuthenticationMethod(Map<String, Object> input) {
		super(input);
		// TODO Auto-generated constructor stub
	}

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "user_id")
	private User user;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}
}
