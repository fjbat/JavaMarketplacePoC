package com.dtme.marketplace.entities;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

@Entity
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = true)
    String password;
    @Column(nullable = true)
    private Date deletedAt;

    @Column(nullable = false)
    private String identifier;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL)
    private List<AuthenticationMethod> authenticationMethods;

    @Column(nullable = false)
    private boolean verified;

    @ManyToMany
    @JoinTable(name = "user_role",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private List<Role> roles;

    @Column(nullable = true)
    private Date lastLogin;

    @Embedded
    private CustomUserFields customFields;

    @OneToMany(mappedBy = "user")
    private List<AuthenticatedSession> sessions;

    public User() {
        // default constructor
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Date deletedAt) {
        this.deletedAt = deletedAt;
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public List<AuthenticationMethod> getAuthenticationMethods() {
        return authenticationMethods;
    }

    public void setAuthenticationMethods(List<AuthenticationMethod> authenticationMethods) {
        this.authenticationMethods = authenticationMethods;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public List<Role> getRoles() {
        return roles;
    }

    public void setRoles(List<Role> roles) {
        this.roles = roles;
    }

    public Date getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(Date lastLogin) {
        this.lastLogin = lastLogin;
    }

    public CustomUserFields getCustomFields() {
        return customFields;
    }

    public void setCustomFields(CustomUserFields customFields) {
        this.customFields = customFields;
    }

    public List<AuthenticatedSession> getSessions() {
        return sessions;
    }

    public void setSessions(List<AuthenticatedSession> sessions) {
        this.sessions = sessions;
    }

    public NativeAuthenticationMethod getNativeAuthenticationMethod() {
        return getNativeAuthenticationMethod(true);
    }

    public NativeAuthenticationMethod getNativeAuthenticationMethod(boolean strict) {
        if (authenticationMethods == null) {
            throw new RuntimeException("Error: User authentication methods not loaded.");
        }
        for (AuthenticationMethod method : authenticationMethods) {
            if (method instanceof NativeAuthenticationMethod) {
                return (NativeAuthenticationMethod) method;
            }
        }
        if (strict) {
            throw new RuntimeException("Error: Native authentication method not found.");
        }
        return null;
    }

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}
}
