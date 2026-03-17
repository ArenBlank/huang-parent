package com.huang.web.app.service.core;

import com.huang.model.entity.UserProfile;
import com.huang.web.app.service.UserProfileService;
import org.springframework.stereotype.Service;

@Service
public class UserProfileCoreService {

    private final UserProfileService userProfileService;

    public UserProfileCoreService(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    public UserProfile getByUserId(Long userId) {
        return userProfileService.getByUserId(userId);
    }

    public void upsertProfile(UserProfile profile) {
        if (profile == null || profile.getUserId() == null) {
            return;
        }
        UserProfile existing = userProfileService.getByUserId(profile.getUserId());
        if (existing == null) {
            userProfileService.save(profile);
            return;
        }
        if (profile.getBio() != null) {
            existing.setBio(profile.getBio());
        }
        if (profile.getAddress() != null) {
            existing.setAddress(profile.getAddress());
        }
        if (profile.getOccupation() != null) {
            existing.setOccupation(profile.getOccupation());
        }
        if (profile.getHeight() != null) {
            existing.setHeight(profile.getHeight());
        }
        if (profile.getWeight() != null) {
            existing.setWeight(profile.getWeight());
        }
        userProfileService.updateById(existing);
    }
}
