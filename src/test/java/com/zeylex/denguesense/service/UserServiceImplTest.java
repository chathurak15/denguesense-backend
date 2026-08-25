package com.zeylex.denguesense.service;

import com.zeylex.denguesense.dto.requestDTO.RegisterDTO;
import com.zeylex.denguesense.model.District;
import com.zeylex.denguesense.model.User;
import com.zeylex.denguesense.model.enums.RoleType;
import com.zeylex.denguesense.repo.DistrictRepo;
import com.zeylex.denguesense.repo.UserRepo;
import com.zeylex.denguesense.service.impl.UserServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService — PHI Telegram registration codes")
class UserServiceImplTest {

    @Mock private UserRepo userRepo;
    @Mock private DistrictRepo districtRepo;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TelegramConnectService telegramConnectService;
    @Spy private ModelMapper modelMapper = new ModelMapper();

    @InjectMocks
    private UserServiceImpl service;

    @Test
    @DisplayName("registering a PHI asks TelegramConnectService to assign a code")
    void registerPhi_assignsTelegramCode() {
        RegisterDTO dto = new RegisterDTO();
        dto.setFname("Test");
        dto.setLname("PHI");
        dto.setEmail("testphi@denguesense.lk");
        dto.setPassword("12345678");
        dto.setRole(RoleType.PHI);
        dto.setDistrictId(1);

        District colombo = new District();
        colombo.setId(1L);
        colombo.setName("Colombo");

        when(userRepo.existsByEmail("testphi@denguesense.lk")).thenReturn(false);
        when(districtRepo.findById(1L)).thenReturn(Optional.of(colombo));
        when(passwordEncoder.encode("12345678")).thenReturn("hashed");
        doAnswer(inv -> {
            User user = inv.getArgument(0);
            user.setTelegramRegistrationCode("PHI-TESTCODE");
            return null;
        }).when(telegramConnectService).assignCodeIfNeeded(any(User.class));
        when(userRepo.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.registerUser(dto)).isEqualTo("User registered successfully");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepo).save(captor.capture());
        assertThat(captor.getValue().getTelegramRegistrationCode()).isEqualTo("PHI-TESTCODE");
        verify(telegramConnectService).assignCodeIfNeeded(any(User.class));
    }

    @Test
    @DisplayName("login mapping includes telegram connect fields")
    void toResponse_appliesTelegramStatus() {
        User phi = new User();
        phi.setId(4L);
        phi.setFname("Test");
        phi.setLname("PHI");
        phi.setEmail("testphi@denguesense.lk");
        phi.setRole(RoleType.PHI);
        phi.setStatus("APPROVED");
        phi.setTelegramRegistrationCode("PHI-9238B766");

        when(userRepo.findByEmail("testphi@denguesense.lk")).thenReturn(phi);
        doAnswer(inv -> {
            inv.getArgument(1, com.zeylex.denguesense.dto.responseDTO.UserResponseDTO.class)
                    .setTelegramConnected(true);
            inv.getArgument(1, com.zeylex.denguesense.dto.responseDTO.UserResponseDTO.class)
                    .setTelegramConnectUrl("https://t.me/denguesensebot?start=PHI-9238B766");
            return null;
        }).when(telegramConnectService).applyTo(any(User.class), any());

        var dto = service.loadUserByUsername("testphi@denguesense.lk");
        assertThat(dto.getTelegramConnected()).isTrue();
        assertThat(dto.getTelegramConnectUrl()).contains("denguesensebot");
    }
}
