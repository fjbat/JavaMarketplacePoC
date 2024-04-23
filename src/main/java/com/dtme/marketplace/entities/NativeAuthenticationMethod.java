package com.dtme.marketplace.entities;

import javax.persistence.Column;
import javax.persistence.Entity;
import com.dtme.marketplace.entities.AuthenticationMethod;

@Entity
public class NativeAuthenticationMethod extends AuthenticationMethod {

	@Column(nullable = false)
	private String identifier;

	@Column(nullable = false)
	private String passwordHash;

	@Column(nullable = true)
	private String verificationToken;

	@Column(nullable = true)
	private String passwordResetToken;

	@Column(nullable = true)
	private String identifierChangeToken;

	@Column(nullable = true)
	private String pendingIdentifier;

	public NativeAuthenticationMethod() {
		super();
	}

	// Getters and setters
	public String getIdentifier() {
		return identifier;
	}

	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public void setPasswordHash(String passwordHash) {
		this.passwordHash = passwordHash;
	}

	public String getVerificationToken() {
		return verificationToken;
	}

	public void setVerificationToken(String verificationToken) {
		this.verificationToken = verificationToken;
	}

	public String getPasswordResetToken() {
		return passwordResetToken;
	}

	public void setPasswordResetToken(String passwordResetToken) {
		this.passwordResetToken = passwordResetToken;
	}

	public String getIdentifierChangeToken() {
		return identifierChangeToken;
	}

	public void setIdentifierChangeToken(String identifierChangeToken) {
		this.identifierChangeToken = identifierChangeToken;
	}

	public String getPendingIdentifier() {
		return pendingIdentifier;
	}

	public void setPendingIdentifier(String pendingIdentifier) {
		this.pendingIdentifier = pendingIdentifier;
	}
}
