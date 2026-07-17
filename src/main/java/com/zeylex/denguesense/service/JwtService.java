package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.LoginDTO;
import com.zeylex.denguesense.dto.responseDTO.LoginResponseDTO;
import com.zeylex.denguesense.dto.responseDTO.UserResponseDTO;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.util.JwtUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Service
public class JwtService implements UserDetailsService {
    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private OtpService otpService;

    private com.zeylex.denguesense.model.User user;


    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        user = userRepo.findByEmail(email);
        if (user != null) {
            String password = user.getPassword() != null ? user.getPassword() : "";
            return new User(user.getEmail(), password, getAuthority(user));
        } else {
            throw new UsernameNotFoundException("User not found: " + email);
        }
    }


    private Set getAuthority(com.zeylex.denguesense.model.User user) {
        Set<GrantedAuthority> grantedAuthorities = new HashSet<>();
        grantedAuthorities.add(new SimpleGrantedAuthority("ROLE_"+ user.getRole()));
        return grantedAuthorities;
    }

// sends OTP, returns a LoginResponseDTO with otpRequired=true
    public LoginResponseDTO createJwtToken(LoginDTO loginDTO) {
        String email = loginDTO.getEmail();
        String password = loginDTO.getPassword();

        try {
            authenticate(email, password);
            UserDetails userDetails = loadUserByUsername(email);

            String token = jwtUtil.generateToken(userDetails);
            UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
            userResponseDTO.setDistrictName(user.getDistrict().getName());
            return new LoginResponseDTO(userResponseDTO, token);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public LoginResponseDTO verifyLoginOtp(String email, String otp) {
        if (!otpService.verifyOtp(email, otp)) {
            return null;
        }
        otpService.clearOtp(email);

        UserDetails userDetails = loadUserByUsername(email);
        String token = jwtUtil.generateToken(userDetails);
        UserResponseDTO userResponseDTO = modelMapper.map(user, UserResponseDTO.class);
        return new LoginResponseDTO(userResponseDTO, token);
    }

    private void authenticate(String email, String password) throws Exception {
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, password));
        } catch (BadCredentialsException e) {
            throw new Exception("Bad credentials", e);
        }
    }

    public String generateTokenByEmail(String email) {
        UserDetails userDetails = loadUserByUsername(email);
        return jwtUtil.generateToken(userDetails);
    }

}
