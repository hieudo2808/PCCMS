package com.astral.express.pccms.user.service;

import com.astral.express.pccms.user.entity.StaffProfile;
import com.astral.express.pccms.user.entity.Users;
import com.astral.express.pccms.user.repository.StaffProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StaffProfileProvisioningService {
    private final StaffProfileRepository staffProfileRepository;

    public void createStaffProfileIfNeeded(Users user) {
        String roleCode = user.getRole() == null ? null : user.getRole().getCode();
        if (!"STAFF".equals(roleCode) && !"VETERINARIAN".equals(roleCode)) {
            return;
        }

        StaffProfile profile = StaffProfile.builder()
                .user(user)
                .professionalTitle(user.getRole().getName())
                .isServiceProvider("VETERINARIAN".equals(roleCode))
                .build();
        staffProfileRepository.save(profile);
    }
}
