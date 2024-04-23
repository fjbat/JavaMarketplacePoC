package com.dtme.marketplace.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dtme.marketplace.entities.User;
import com.dtme.marketplace.repos.UserRepository;
import com.dtme.marketplace.utils.PasswordEncoder;

import java.util.Optional;

@Service
@Transactional
public class UserService2 {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleService roleService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    public Optional<User> getUserByEmailAddress(String emailAddress) {
        return userRepository.findByEmailAddress(emailAddress);
    }

    public User createCustomerUser(String identifier, String password) {
        User user = new User();
        user.setIdentifier(identifier);
        user.setRoles(Arrays.asList(roleService.getCustomerRole()));
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public User createAdminUser(String identifier, String password) {
        User user = new User();
        user.setIdentifier(identifier);
        user.setRoles(Arrays.asList(roleService.getAdminRole()));
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public void softDelete(Long userId) {
        userRepository.deleteById(userId);
    }

    public User setVerificationToken(User user) {
        user.setVerificationToken(verificationTokenGenerator.generateVerificationToken());
        user.setVerified(false);
        return userRepository.save(user);
    }

    public User verifyUserByToken(String verificationToken, String password) {
        User user = userRepository.findByVerificationToken(verificationToken);
        if (user != null) {
            if (verificationTokenGenerator.verifyVerificationToken(verificationToken)) {
                user.setVerified(true);
                user.setPassword(passwordEncoder.encode(password));
                return userRepository.save(user);
            } else {
                throw new VerificationTokenExpiredException("Verification token expired");
            }
        } else {
            throw new VerificationTokenInvalidException("Invalid verification token");
        }
    }

    public User setPasswordResetToken(String emailAddress) {
        User user = userRepository.findByEmailAddress(emailAddress);
        if (user != null) {
            user.setPasswordResetToken(verificationTokenGenerator.generateVerificationToken());
            return userRepository.save(user);
        } else {
            throw new UserNotFoundException("User with email address not found");
        }
    }

    public User resetPasswordByToken(String passwordResetToken, String password) {
        User user = userRepository.findByPasswordResetToken(passwordResetToken);
        if (user != null) {
            if (verificationTokenGenerator.verifyVerificationToken(passwordResetToken)) {
                user.setPassword(passwordEncoder.encode(password));
                user.setPasswordResetToken(null);
                return userRepository.save(user);
            } else {
                throw new PasswordResetTokenExpiredException("Password reset token expired");
            }
        } else {
            throw new PasswordResetTokenInvalidException("Invalid password reset token");
        }
    }

    public boolean updatePassword(Long userId, String currentPassword, String newPassword) {
        Optional<User> userOptional = userRepository.findById(userId);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            if (passwordEncoder.matches(currentPassword, user.getPassword())) {
                user.setPassword(passwordEncoder.encode(newPassword));
                userRepository.save(user);
                return true;
            } else {
                throw new InvalidCredentialsException("Invalid credentials");
            }
        } else {
            throw new UserNotFoundException("User not found");
        }
    }
}