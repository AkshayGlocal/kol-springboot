package com.kol.kol.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.kol.kol.model.AppUser;

@Repository
public interface AppUserRepo extends JpaRepository<AppUser,Long>{
    AppUser findByEmail(String email);

}