package tbh.chat_app.config.jwt;

import tbh.chat_app.model.User;
import tbh.chat_app.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email).block();
        return UserPrincipal.create(user); // custom UserPrincipal implements UserDetails
    }


}
