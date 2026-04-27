package com.medsync.service;

import com.medsync.dto.AuthDto;
import com.medsync.entity.Hospital;
import com.medsync.entity.User;
import com.medsync.repository.HospitalRepository;
import com.medsync.repository.UserRepository;
import com.medsync.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final HospitalRepository hospitalRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;

    @Transactional
    public AuthDto.MessageResponse registerHospital(AuthDto.RegisterHospitalRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new RuntimeException("Username already taken");
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        if (hospitalRepository.findByLicenseNumber(req.getLicenseNumber()).isPresent()) {
            throw new RuntimeException("License number already registered");
        }

        User user = User.builder()
                .username(req.getUsername())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getType() == Hospital.InstitutionType.PHARMACY ? User.Role.PHARMACY : User.Role.HOSPITAL)
                .status(User.AccountStatus.PENDING)
                .build();
        userRepository.save(user);

        Hospital hospital = Hospital.builder()
                .user(user)
                .hospitalName(req.getHospitalName())
                .address(req.getAddress())
                .licenseNumber(req.getLicenseNumber())
                .contactPhone(req.getContactPhone())
                .contactEmail(req.getContactEmail())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .type(req.getType())
                .shareExpiryAlerts(false)
                .build();
        hospitalRepository.save(hospital);

        auditService.log(null, "REGISTER", "Hospital", hospital.getId(),
                "New registration: " + req.getHospitalName());

        return AuthDto.MessageResponse.builder()
                .success(true)
                .message("Registration submitted. Awaiting city admin approval.")
                .build();
    }

    public AuthDto.AuthResponse login(AuthDto.LoginRequest req) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Invalid credentials");
        } catch (DisabledException e) {
            throw new RuntimeException("Account not yet activated by admin");
        }

        User user = userRepository.findByUsername(req.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getStatus() == User.AccountStatus.PENDING) {
            throw new RuntimeException("Account pending approval");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(req.getUsername());

        Map<String, Object> claims = new HashMap<>();
        claims.put("role", user.getRole().name());

        String token = jwtUtils.generateToken(userDetails, claims);

        Hospital hospital = null;
        try {
            hospital = hospitalRepository.findByUser(user).orElse(null);
        } catch (Exception ignored) {}

        auditService.log(user, "LOGIN", "User", user.getId(), "User logged in");

        return AuthDto.AuthResponse.builder()
                .token(token)
                .username(user.getUsername())
                .role(user.getRole().name())
                .institutionId(hospital != null ? hospital.getId() : null)
                .institutionName(hospital != null ? hospital.getHospitalName() : "City Admin")
                .status(user.getStatus().name())
                .build();
    }
}
