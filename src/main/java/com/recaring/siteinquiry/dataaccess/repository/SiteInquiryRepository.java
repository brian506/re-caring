package com.recaring.siteinquiry.dataaccess.repository;

import com.recaring.siteinquiry.dataaccess.entity.SiteInquiry;
import com.recaring.siteinquiry.dataaccess.entity.SiteInquiryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SiteInquiryRepository extends JpaRepository<SiteInquiry, Long> {

    Optional<SiteInquiry> findByInquiryKey(String inquiryKey);

    List<SiteInquiry> findAllByOrderByIdDesc();

    List<SiteInquiry> findByStatusOrderByIdAsc(SiteInquiryStatus status);
}
