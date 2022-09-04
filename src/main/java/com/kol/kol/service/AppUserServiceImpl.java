package com.kol.kol.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.transaction.Transactional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.kol.kol.model.AppUser;
import com.kol.kol.model.RequestProfile;
import com.kol.kol.model.Role;
import com.kol.kol.repo.AppUserRepo;
import com.kol.kol.repo.RequestProfileRepo;
import com.kol.kol.repo.RoleRepo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.asm.Advice.Local;


@Service
@RequiredArgsConstructor
@Transactional
@Slf4j

public class AppUserServiceImpl implements AppUserService,UserDetailsService{

    
    private final AppUserRepo appUserRepo;
    private final RoleRepo roleRepo;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;
    private final RequestProfileRepo requestProfileRepo;


    @Override
    public RequestProfile getRequestProfileProvidedToken(String token) {
        return requestProfileRepo.findByToken(token);
    }

    @Override
    public RequestProfile getRequestProfile(String kolid) {
        return  requestProfileRepo.findByKolProfileId(kolid);
    }

    @Override
    public int updateApprovedAtToken(String token) {
        return requestProfileRepo.updateApprovedAt(token, LocalDateTime.now());
    }

    @Override
    public RequestProfile getRequestProfiletoken(String token) {
        return requestProfileRepo.findByToken(token);
    }

    @Override
    public void deleteRequestProfileByKOLID(String KolProfileId) {
        requestProfileRepo.deleteByKolProfileId(KolProfileId);
    }

    @Override
    public RequestProfile saveRequestProfile(RequestProfile requestProfile) {
        log.info("KolProfileID-> "+requestProfile.getKolProfileId()+" saved");
        return requestProfileRepo.save(requestProfile);
    }
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        AppUser appUser = appUserRepo.findByEmail(email);
        if(appUser == null){
            log.error("user {} not found in the database",email);
            throw new UsernameNotFoundException("user not found in the database");
        }else{
            log.info("User->{} found in the database",appUser.getUsername());
        }
        Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
        appUser.getRoles().forEach(role->{
            authorities.add(new SimpleGrantedAuthority(role.getName()));
        });
        return new org.springframework.security.core.userdetails.User(appUser.getEmail(), appUser.getPassword(),authorities);
    }

    @Override
    public AppUser saveAppUser(AppUser appUser) {
        log.info("appuser-> {} saved",appUser.getEmail());
        String encodedPassword = bCryptPasswordEncoder.encode(appUser.getPassword());
        appUser.setPassword(encodedPassword);
        return appUserRepo.save(appUser);
    }

    @Override
    public Role saveRole(Role role) {
        log.info("role-> {} saved ",role.getName());
        return roleRepo.save(role);
    }

    @Override
    public void addRoleToAppUser(String email, String roleName) {
        //TODO in production add more logic to validate
        AppUser appUser = appUserRepo.findByEmail(email);
        Role role = roleRepo.findByName(roleName);
        log.info("role-> {} added to user-> {}",roleName,email);
        appUser.getRoles().add(role);
    }

    @Override
    public AppUser getAppUser(String email) {
        log.info("fetching user {}",email);
        
        return appUserRepo.findByEmail(email);
    }

    @Override
    public List<AppUser> getAppUsers() {
        log.info("fetching all users");
        return appUserRepo.findAll();
    }    
    
}
