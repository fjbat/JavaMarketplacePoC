import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpServerErrorException.InternalServerError;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.NativeAuthenticationMethod;
import com.dtme.marketplace.entities.User;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserService {

	@PersistenceContext
	private EntityManager entityManager;

	private final TransactionalConnection connection;
	private final ConfigService configService;
	private final RoleService roleService;
	private final PasswordCipher passwordCipher;
	private final VerificationTokenGenerator verificationTokenGenerator;
	private final ModuleRef moduleRef;

	@Autowired
	public UserService(TransactionalConnection connection, ConfigService configService, RoleService roleService,
			PasswordCipher passwordCipher, VerificationTokenGenerator verificationTokenGenerator, ModuleRef moduleRef) {
		this.connection = connection;
		this.configService = configService;
		this.roleService = roleService;
		this.passwordCipher = passwordCipher;
		this.verificationTokenGenerator = verificationTokenGenerator;
		this.moduleRef = moduleRef;
	}

	public Optional<User> getUserById(RequestContext ctx, ID userId) {
		return Optional.ofNullable(entityManager.find(User.class, userId));
	}

	public Optional<User> getUserByEmailAddress(RequestContext ctx, String emailAddress, String userType) {
		String entity = userType != null ? userType : (ctx.getApiType().equals("admin") ? "administrator" : "customer");
		String table = (configService.getDbConnectionOptions().entityPrefix != null
				? configService.getDbConnectionOptions().entityPrefix
				: "") + entity;

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<User> cq = cb.createQuery(User.class);
		Root<User> userRoot = cq.from(User.class);
		Join<User, Role> rolesJoin = userRoot.join("roles");
		Join<Role, Channel> channelsJoin = rolesJoin.join("channels");
		Join<User, AuthenticationMethod> authenticationMethodsJoin = userRoot.join("authenticationMethods");

		cq.where(cb.isNull(userRoot.get("deletedAt")));

		if (isEmailAddressLike(emailAddress)) {
			cq.where(cb.equal(cb.lower(userRoot.get("identifier")), normalizeEmailAddress(emailAddress)));
		} else {
			cq.where(cb.equal(userRoot.get("identifier"), emailAddress));
		}

		return Optional.ofNullable(entityManager.createQuery(cq).getSingleResult());
	}

	@Transactional
	public Optional<User> createCustomerUser(RequestContext ctx, String identifier, String password) {
		User user = new User();
		user.setIdentifier(normalizeEmailAddress(identifier));
		Role customerRole = roleService.getCustomerRole(ctx).orElseThrow();
		user.setRoles(List.of(customerRole));
		Optional<User> userResult = addNativeAuthenticationMethod(ctx, user, identifier, password);
		return userResult.map(u -> entityManager.merge(u));
	}

	@Transactional
	public Optional<User> addNativeAuthenticationMethod(RequestContext ctx, User user, String identifier,
			String password) {
		Optional<User> checkUser = getUserById(ctx, user.getId());
		if (checkUser.isPresent()) {
			if (checkUser.get().getAuthenticationMethods().stream()
					.anyMatch(m -> m instanceof NativeAuthenticationMethod)) {
				return Optional.of(user);
			}
		}
		NativeAuthenticationMethod authenticationMethod = new NativeAuthenticationMethod();
		if (configService.getAuthOptions().requireVerification) {
			authenticationMethod.setVerificationToken(verificationTokenGenerator.generateVerificationToken());
			user.setVerified(false);
		} else {
			user.setVerified(true);
		}
		if (password != null) {
			Optional<PasswordValidationError> passwordValidationResult = validatePassword(ctx, password);
			if (passwordValidationResult.isPresent()) {
				return Optional.empty(); // Return empty Optional in case of validation error
			}
			authenticationMethod.setPasswordHash(passwordCipher.hash(password));
		} else {
			authenticationMethod.setPasswordHash("");
		}
		authenticationMethod.setIdentifier(normalizeEmailAddress(identifier));
		authenticationMethod.setUser(user);
		entityManager.persist(authenticationMethod);
		user.setAuthenticationMethods(
				user.getAuthenticationMethods() != null ? user.getAuthenticationMethods() : List.of());
		user.getAuthenticationMethods().add(authenticationMethod);
		return Optional.of(user);
	}

	
	 @Transactional
	    public Optional<User> createAdminUser(RequestContext ctx, String identifier, String password) {
	        User user = new User();
	        user.setIdentifier(normalizeEmailAddress(identifier));
	        user.setVerified(true);
	        
	        NativeAuthenticationMethod authenticationMethod = new NativeAuthenticationMethod();
	        authenticationMethod.setIdentifier(normalizeEmailAddress(identifier));
	        authenticationMethod.setPasswordHash(passwordCipher.hash(password).orElseThrow());
	        user.setAuthenticationMethods(List.of(authenticationMethod));

	        return Optional.of(entityManager.merge(user));
	    }
	  @Transactional
	    public void softDelete(RequestContext ctx, ID userId) {
	        // Dynamic import to avoid the circular dependency of SessionService
	        SessionService sessionService = (SessionService) this.connection.getModuleRef().get(SessionService.class);
	        sessionService.deleteSessionsByUser(ctx, new User(userId));

	        // Get the User entity
	        User user = entityManager.find(User.class, userId);
	        if (user != null) {
	            // Update the deletedAt field
	            user.setDeletedAt(new Date());
	            entityManager.merge(user);
	        }
	    }

	    @Transactional
	    public Optional<User> setVerificationToken(RequestContext ctx, User user) {
	        NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();
	        if (nativeAuthMethod != null) {
	            nativeAuthMethod.setVerificationToken(verificationTokenGenerator.generateVerificationToken());
	            user.setVerified(false);
	            entityManager.merge(nativeAuthMethod);
	            return Optional.of(entityManager.merge(user));
	        } else {
	            return Optional.empty(); // Return empty optional if no native authentication method found
	        }
	    }
	    @Transactional
	    public Optional<?> verifyUserByToken(RequestContext ctx, String verificationToken, String password) {
	        // Use JPQL query to retrieve the user by verification token
	        User user = entityManager.createQuery(
	                "SELECT u FROM User u " +
	                        "LEFT JOIN FETCH u.authenticationMethods a " +
	                        "WHERE a.verificationToken = :verificationToken", User.class)
	                .setParameter("verificationToken", verificationToken)
	                .getSingleResult();

	        if (user != null) {
	            NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();
	            if (nativeAuthMethod == null) {
	                return Optional.empty(); // Return empty optional if no native authentication method found
	            }

	            if (verificationTokenGenerator.verifyVerificationToken(verificationToken)) {
	                if (password == null) {
	                    if (nativeAuthMethod.getPasswordHash() == null) {
	                        return Optional.of(new MissingPasswordError());
	                    }
	                } else {
	                    if (nativeAuthMethod.getPasswordHash() != null) {
	                        return Optional.of(new PasswordAlreadySetError());
	                    }
	                    // Validate password
	                    // Implement validatePassword method
	                    PasswordValidationError passwordValidationResult = validatePassword(ctx, password);
	                    if (passwordValidationResult != null) {
	                        return Optional.of(passwordValidationResult);
	                    }
	                    // Hash password
	                    String hashedPassword = passwordCipher.hash(password);
	                    nativeAuthMethod.setPasswordHash(hashedPassword);
	                }
	                nativeAuthMethod.setVerificationToken(null);
	                user.setVerified(true);
	                entityManager.merge(nativeAuthMethod);
	                return Optional.of(entityManager.merge(user));
	            } else {
	                return Optional.of(new VerificationTokenExpiredError());
	            }
	        } else {
	            return Optional.of(new VerificationTokenInvalidError());
	        }
	    }

	    @Transactional
	    public Optional<User> setPasswordResetToken(RequestContext ctx, String emailAddress) {
	        // Call the method to retrieve user by email address
	        Optional<User> userOptional = getUserByEmailAddress(ctx, emailAddress);
	        if (userOptional.isPresent()) {
	            User user = userOptional.get();
	            NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod(false);
	            if (nativeAuthMethod != null) {
	                nativeAuthMethod.setPasswordResetToken(verificationTokenGenerator.generateVerificationToken());
	                entityManager.merge(nativeAuthMethod);
	                return Optional.of(entityManager.merge(user));
	            } else {
	                return Optional.empty(); // Return empty optional if no native authentication method found
	            }
	        } else {
	            return Optional.empty(); // Return empty optional if user not found
	        }
	    }
	    @Transactional
	    public Optional<?> resetPasswordByToken(RequestContext ctx, String passwordResetToken, String password) {
	        // Use JPQL query to retrieve the user by password reset token
	        User user = entityManager.createQuery(
	                "SELECT u FROM User u " +
	                        "LEFT JOIN FETCH u.authenticationMethods a " +
	                        "WHERE a.passwordResetToken = :passwordResetToken", User.class)
	                .setParameter("passwordResetToken", passwordResetToken)
	                .getSingleResult();

	        if (user != null) {
	            // Validate password
	            PasswordValidationError passwordValidationResult = validatePassword(ctx, password);
	            if (passwordValidationResult != null) {
	                return Optional.of(passwordValidationResult);
	            }
	            if (verificationTokenGenerator.verifyVerificationToken(passwordResetToken)) {
	                NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();
	                // Hash password
	                String hashedPassword = passwordCipher.hash(password);
	                nativeAuthMethod.setPasswordHash(hashedPassword);
	                nativeAuthMethod.setPasswordResetToken(null);
	                entityManager.merge(nativeAuthMethod);
	                if (!user.isVerified() && this.configService.getAuthOptions().isRequireVerification()) {
	                    // Set user as verified if required
	                    user.setVerified(true);
	                }
	                return Optional.of(entityManager.merge(user));
	            } else {
	                return Optional.of(new PasswordResetTokenExpiredError());
	            }
	        } else {
	            return Optional.of(new PasswordResetTokenInvalidError());
	        }
	    }
	    @Transactional
	    public void changeUserAndNativeIdentifier(RequestContext ctx, ID userId, String newIdentifier) {
	        // Retrieve user by ID
	        User user = entityManager.find(User.class, userId);

	        if (user != null) {
	            // Find native authentication method
	            NativeAuthenticationMethod nativeAuthMethod = user.getAuthenticationMethods().stream()
	                    .filter(m -> m instanceof NativeAuthenticationMethod)
	                    .map(m -> (NativeAuthenticationMethod) m)
	                    .findFirst()
	                    .orElse(null);

	            if (nativeAuthMethod != null) {
	                // Update native authentication method
	                nativeAuthMethod.setIdentifier(newIdentifier);
	                nativeAuthMethod.setIdentifierChangeToken(null);
	                entityManager.merge(nativeAuthMethod);
	            }

	            // Update user identifier
	            user.setIdentifier(newIdentifier);
	            entityManager.merge(user);
	        }
	    }

	    @Transactional
	    public Optional<User> setIdentifierChangeToken(RequestContext ctx, User user) {
	        // Retrieve native authentication method
	        NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();

	        if (nativeAuthMethod != null) {
	            // Generate verification token and set it to identifier change token
	            nativeAuthMethod.setIdentifierChangeToken(verificationTokenGenerator.generateVerificationToken());
	            entityManager.merge(nativeAuthMethod);
	            return Optional.of(entityManager.merge(user));
	        } else {
	            return Optional.empty();
	        }
	    }
	    @Transactional
	    public Optional<ChangeIdentifierResult> changeIdentifierByToken(RequestContext ctx, String token) {
	        // Retrieve user by identifier change token
	        User user = entityManager.createQuery(
	                "SELECT user FROM User user " +
	                        "LEFT JOIN FETCH user.authenticationMethods aums " +
	                        "LEFT JOIN aums.identifierChangeToken authenticationMethod " +
	                        "WHERE authenticationMethod = :identifierChangeToken", User.class)
	                .setParameter("identifierChangeToken", token)
	                .getResultList()
	                .stream()
	                .findFirst()
	                .orElse(null);

	        if (user == null) {
	            return Optional.of(new ChangeIdentifierResult(new IdentifierChangeTokenInvalidError()));
	        }

	        if (!verificationTokenGenerator.verifyVerificationToken(token)) {
	            return Optional.of(new ChangeIdentifierResult(new IdentifierChangeTokenExpiredError()));
	        }

	        NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();

	        if (nativeAuthMethod == null || nativeAuthMethod.getPendingIdentifier() == null) {
	            throw new InternalServerError("error.pending-identifier-missing");
	        }

	        String oldIdentifier = user.getIdentifier();
	        String pendingIdentifier = nativeAuthMethod.getPendingIdentifier();

	        user.setIdentifier(pendingIdentifier);
	        nativeAuthMethod.setIdentifier(pendingIdentifier);
	        nativeAuthMethod.setIdentifierChangeToken(null);
	        nativeAuthMethod.setPendingIdentifier(null);

	        entityManager.merge(nativeAuthMethod);
	        entityManager.merge(user);

	        return Optional.of(new ChangeIdentifierResult(user, oldIdentifier));
	    }

	    @Transactional
	    public boolean updatePassword(RequestContext ctx, ID userId, String currentPassword, String newPassword) {
	        // Retrieve user by ID
	        User user = entityManager.createQuery(
	                "SELECT user FROM User user " +
	                        "LEFT JOIN FETCH user.authenticationMethods authenticationMethods " +
	                        "WHERE user.id = :userId", User.class)
	                .setParameter("userId", userId)
	                .getSingleResult();

	        if (user == null) {
	            throw new EntityNotFoundError("User", userId);
	        }

	        String password = newPassword;

	        // Validate new password
	        PasswordValidationResult passwordValidationResult = validatePassword(ctx, password);

	        if (!passwordValidationResult.isSuccess()) {
	            return passwordValidationResult.getError();
	        }

	        NativeAuthenticationMethod nativeAuthMethod = user.getNativeAuthenticationMethod();

	        if (nativeAuthMethod == null) {
	            throw new InternalServerError("error.no-native-auth-method-found");
	        }

	        // Check if current password matches
	        if (!passwordCipher.check(currentPassword, nativeAuthMethod.getPasswordHash())) {
	            return new InvalidCredentialsError();
	        }

	        // Update password
	        nativeAuthMethod.setPasswordHash(passwordCipher.hash(newPassword));
	        entityManager.merge(nativeAuthMethod);

	        return true;
	    }
	    public PasswordValidationResult validatePassword(RequestContext ctx, String password) {
	        PasswordValidationResult passwordValidationResult = authOptions.getPasswordValidationStrategy().validate(ctx, password);
	        if (!passwordValidationResult.isSuccess()) {
	            String message = (passwordValidationResult.getMessage() != null) ? passwordValidationResult.getMessage() : "Password is invalid";
	            return new PasswordValidationResult(false, message);
	        }
	        return new PasswordValidationResult(true, null);
	    }
	 

}
