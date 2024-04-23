package com.dtme.marketplace.controllers;
import java.util.ArrayList;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.RequestContext;

import com.dtme.marketplace.entities.User;
import com.dtme.marketplace.entities.AuthenticationMethod;
import com.dtme.marketplace.service.UserService;



@Component
public class UserEntityResolver {

    private final UserService userService;

    @Autowired
    public UserEntityResolver(UserService userService) {
        this.userService = userService;
    }

    public List<AuthenticationMethod> authenticationMethods(RequestContext ctx, User user) {
        List<AuthenticationMethod> authenticationMethods = new ArrayList<AuthenticationMethod>();
        if (user.getAuthenticationMethods() != null) {
            authenticationMethods.addAll(user.getAuthenticationMethods());
        }
        User userWithMethods = userService.getUserById(ctx, user.getId());
        if (userWithMethods != null && userWithMethods.getAuthenticationMethods() != null) {
            authenticationMethods.addAll(userWithMethods.getAuthenticationMethods());
        }

        List<AuthenticationMethodType> result = new ArrayList<>();
        for (AuthenticationMethod method : authenticationMethods) {
            AuthenticationMethodType authenticationMethodType = new AuthenticationMethodType();
            authenticationMethodType.setId(method.getId().toString());
            if (method instanceof ExternalAuthenticationMethod) {
                ExternalAuthenticationMethod externalMethod = (ExternalAuthenticationMethod) method;
                authenticationMethodType.setStrategy(externalMethod.getStrategy());
            } else {
                authenticationMethodType.setStrategy(NativeAuthenticationStrategy.NAME);
            }
            // You may need to set other fields based on your requirements
            result.add(authenticationMethodType);
        }
        return result;
    }
}
