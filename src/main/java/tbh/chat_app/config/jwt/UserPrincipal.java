package tbh.chat_app.config.jwt;

import java.util.*;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import tbh.chat_app.model.User;

public class UserPrincipal implements UserDetails, OAuth2User {
    private String id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    // OAuth2 attribbutes
    private Map<String, Object> attributes;

    public UserPrincipal(String id, String email, String password, Collection<? extends GrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public String getId() {
        return id;
    }

    // Implement UserDetails methods
    @Override
    public String getUsername() { return email; }

    @Override
    public String getPassword() { return password; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    // Implement OAuth2User methods
    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return String.valueOf(id);
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    // Add custom static method
    public static UserPrincipal create(User user) {
       List<GrantedAuthority> authorities = Optional.ofNullable(user.getRoles())
        .orElse(List.of())
        .stream()
        .map(role -> new SimpleGrantedAuthority(role.name()))
        .collect(Collectors.toList());


        return new UserPrincipal(
                user.getUserId(),
                user.getEmail(),
                user.getPassword(),
                authorities
        );
    }

     public static UserPrincipal create(User user, Map<String, Object> attributes) {
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        userPrincipal.setAttributes(attributes);
        return userPrincipal;
    }

}
