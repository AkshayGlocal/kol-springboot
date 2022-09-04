package com.kol.kol.service;

import java.util.List;

import com.kol.kol.model.AppUser;
import com.kol.kol.model.RequestProfile;
import com.kol.kol.model.Role;

public interface AppUserService {
    AppUser saveAppUser(AppUser appUser);
    Role saveRole(Role role);
    //TODO check if the user already exists
    void addRoleToAppUser(String email,String roleName);
    AppUser getAppUser(String email);
    // TODO implement Pageable to optimize
    List<AppUser> getAppUsers();
    RequestProfile saveRequestProfile(RequestProfile requestProfile);
    RequestProfile getRequestProfileProvidedToken(String token);

    RequestProfile getRequestProfile(String kolid);
    int updateApprovedAtToken (String token);

    RequestProfile getRequestProfiletoken(String token);

    void deleteRequestProfileByKOLID(String KolProfileId);

}
