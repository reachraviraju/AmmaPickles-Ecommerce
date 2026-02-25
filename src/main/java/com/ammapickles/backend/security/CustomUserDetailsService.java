package com.ammapickles.backend.security;

import com.ammapickles.backend.entity.User;
import com.ammapickles.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        Set<GrantedAuthority> authorities = user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
        
  
        
        

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isEnabled(),
                true,     // accountNonExpired
                true,              // credentialsNonExpired
                true,   // accountNonLocked
                authorities 
                
                
        );
    }
}

