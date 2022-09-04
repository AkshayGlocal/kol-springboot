package com.kol.kol.repo;



import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.kol.kol.model.RequestProfile;


@Repository
@Transactional(readOnly=true)
public interface RequestProfileRepo extends JpaRepository<RequestProfile,Long>{
    RequestProfile findByUsername(String username);
    RequestProfile findByToken(String token);

    void deleteByKolProfileId(String kolProfileId);

    RequestProfile findByKolProfileId(String kolProfileId);
    @Transactional
    @Modifying
    @Query("UPDATE RequestProfile r "+
            "SET r.approvedAt = ?2 "+
            "WHERE r.token = ?1")
    int updateApprovedAt(String token,LocalDateTime approvedAt);


}
