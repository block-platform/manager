package com.manager.app.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;

import static com.manager.app.security.SecurityConstants.HEADER_STRING;
import static com.manager.app.security.SecurityConstants.SECRET;
import static com.manager.app.security.SecurityConstants.TOKEN_PREFIX;

public class JWTAuthorizationFilter extends BasicAuthenticationFilter {

    public JWTAuthorizationFilter(AuthenticationManager authManager) {
        super(authManager);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse res,
                                    FilterChain chain) throws IOException, ServletException {
        String header = req.getHeader(HEADER_STRING);

        if (header == null || !header.startsWith(TOKEN_PREFIX)) {
            chain.doFilter(req, res);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(req);

        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(req, res);
    }

    // Reads the JWT from the Authorization header, and then uses JWT to validate the token
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader(HEADER_STRING);
        try{
            if (token != null) {
                // parse the token.
                String user = JWT.require(Algorithm.HMAC512(SECRET.getBytes()))
                        .build()
                        .verify(token.replace(TOKEN_PREFIX, ""))
                        .getSubject();

                if (user != null) {
                    // new arraylist means authorities
                    return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
                }

                return null;
            }
        } catch (TokenExpiredException e){
            System.out.println("Authentication issue!! " + e.getMessage());
        }
        return null;
    }
}