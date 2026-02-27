package com.ammapickles.backend.security;

import com.ammapickles.backend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;


                                                             // To Access full user info (id, phone, etc.) anywhere without extra DB call 
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    
    private final User user;

  
    // reads getAuthorities() on every secured request
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return user.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toSet());
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

 
   
    // Since your app uses EMAIL as login, we return email here
    //  email-based auth
    @Override
    public String getUsername() {
        return user.getEmail();
    }
 
      // Account status flags

    @Override
    public boolean isAccountNonExpired() {
        return true;  
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;  //  return user.isNonLocked() after failed login tracking
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
      
        return user.isEnabled();
    }




    //   CustomUserDetails userDetails = (CustomUserDetails) SecurityContextHolder...
    //   Long userId = userDetails.getId();  no extra DB call needed
    public Long getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getPhoneNumber() {
        return user.getPhoneNumber();
    }

     // Access full user entity
    public User getUser() {
        return user;
    }
}