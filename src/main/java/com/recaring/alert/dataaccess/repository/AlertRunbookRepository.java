package com.recaring.alert.dataaccess.repository;

import com.recaring.alert.dataaccess.entity.AlertRunbook;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AlertRunbookRepository extends JpaRepository<AlertRunbook, Long> {

    @Query(value = """
            SELECT * FROM alert_runbooks
            WHERE deleted_at IS NULL
              AND is_valid = TRUE
              AND alert_name = :alertName
              AND to_tsvector('english', error_signature) @@ plainto_tsquery('english', :errorSignature)
            ORDER BY success_count DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<AlertRunbook> findByAlertNameAndErrorSignature(
            @Param("alertName") String alertName,
            @Param("errorSignature") String errorSignature);
}
