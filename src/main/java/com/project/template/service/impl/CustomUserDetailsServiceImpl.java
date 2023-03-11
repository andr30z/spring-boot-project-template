package com.project.template.service.impl;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.project.template.dto.CustomUserDetails;
import com.project.template.exception.ResourceNotFoundException;
import com.project.template.model.User;
import com.project.template.repository.UserRepository;

@Service
public class CustomUserDetailsServiceImpl implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String s) throws UsernameNotFoundException {
        User user =
                userRepository.findUserByEmail(s).orElseThrow(() -> new ResourceNotFoundException("User not found with email " + s));
        return new CustomUserDetails(user);
    }
}