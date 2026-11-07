package org.example.authservice.service.impl;

import lombok.AllArgsConstructor;
import org.example.authservice.repository.RoleRepository;
import org.example.authservice.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;



@AllArgsConstructor
@Service
public class UserServiceImpl{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

}
