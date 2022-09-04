package com.kol.kol.filters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.client.HttpClientErrorException.Forbidden;
import org.springframework.web.filter.OncePerRequestFilter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import static java.util.Arrays.stream;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@Slf4j
public class CustomAuthorizationfilter extends OncePerRequestFilter{

    private String secret_key="2G/pe/o+APbIKXtZHBHem/15fDvr9rLT+5dqvKh/Qz4=";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
    HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
    // HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    // httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
    // httpServletResponse.setHeader("Access-Control-Allow-Methods", "*");
    // httpServletResponse.setHeader("Access-Control-Allow-Headers", "*");
    // httpServletResponse.setHeader("Access-Control-Allow-Credentials", "*");
    // httpServletResponse.setHeader("Access-Control-Max-Age", "3600");
    // filterChain.doFilter(request, response);
  //  response.setHeader("Access-Control-Allow-Origin","*");

        if(request.getServletPath().equals("/api/v1/login") || 
        request.getServletPath().equals("/api/v1/refresh/token"))
        {
            filterChain.doFilter(request, response);
        }else{
            String authorizationHeader = request.getHeader(AUTHORIZATION);
            if(authorizationHeader !=null && authorizationHeader.startsWith("Bearer ")){
                try {
                    String token=authorizationHeader.substring("Bearer ".length());
                    Algorithm algorithm = Algorithm.HMAC256(secret_key.getBytes());
                    JWTVerifier verifier = JWT.require(algorithm).build();
                    DecodedJWT decodedJWT = verifier.verify(token);
                    String username = decodedJWT.getSubject();
                    String[] roles = decodedJWT.getClaim("roles").asArray(String.class);
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    stream(roles).forEach(role->{
                        authorities.add(new SimpleGrantedAuthority(role));
                    });
                    UsernamePasswordAuthenticationToken authenticationToken = 
                    new UsernamePasswordAuthenticationToken(username, null,authorities);
                    SecurityContextHolder.getContext().setAuthentication(authenticationToken);
                    filterChain.doFilter(request, response);
                } catch (Exception exception) {
                    log.error("Error logging in: {}",exception.getMessage());
                    response.setHeader("error",exception.getMessage());
                    response.setStatus(FORBIDDEN.value());
                   // response.sendError(FORBIDDEN.value());
                   //  response.setContentType(APPLICATION_JSON_VALUE);
                    Map<String,String> error = new HashMap<>();
                    error.put("Error-> ",exception.getMessage());
                    new ObjectMapper().writeValue(response.getOutputStream(), error);
                    
                }    
            }else{
                filterChain.doFilter(request, response);
            }


            
        }
        
    }
    
    
}
