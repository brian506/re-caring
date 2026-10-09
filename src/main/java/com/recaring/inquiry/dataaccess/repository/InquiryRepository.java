package com.recaring.inquiry.dataaccess.repository;

import com.recaring.inquiry.dataaccess.entity.Inquiry;
import com.recaring.inquiry.dataaccess.entity.InquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    Optional<Inquiry> findByInquiryKey(String inquiryKey);

    List<Inquiry> findByMemberKeyOrderByIdDesc(String memberKey);

    List<Inquiry> findByStatusOrderByIdAsc(InquiryStatus status);

    List<Inquiry> findAllByOrderByIdDesc();

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM Inquiry i WHERE i.memberKey = :memberKey")
    void deleteByMemberKey(@Param("memberKey") String memberKey);
}
