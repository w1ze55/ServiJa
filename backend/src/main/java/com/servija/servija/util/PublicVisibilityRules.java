package com.servija.servija.util;

import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.User;

public final class PublicVisibilityRules {

    private PublicVisibilityRules() {
    }

    public static boolean isUserPublic(User user) {
        return user != null
                && Boolean.TRUE.equals(user.getActive())
                && user.getDeletedAt() == null;
    }

    public static boolean isFreelancerPublic(Freelancer freelancer) {
        return isUserPublic(freelancer);
    }

    public static boolean isServicePublic(Service service) {
        return service != null
                && Boolean.TRUE.equals(service.getIsActive())
                && isFreelancerPublic(service.getFreelancer());
    }
}
