package com.recaring.alert.dataaccess.entity;

import com.recaring.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

import java.util.List;

// TODO: CREATE INDEX idx_alert_runbooks_alert_name ON alert_runbooks(alert_name);
// TODO: CREATE INDEX idx_alert_runbooks_is_valid ON alert_runbooks(is_valid) WHERE deleted_at IS NULL;
// TODO: CREATE INDEX idx_alert_runbooks_error_signature_fts ON alert_runbooks USING GIN (to_tsvector('english', error_signature)) WHERE deleted_at IS NULL;
@Getter
@Entity
@Table(name = "alert_runbooks")
@SQLDelete(sql = "UPDATE alert_runbooks SET deleted_at = NOW() WHERE alert_runbook_id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertRunbook extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_runbook_id")
    private Long id;

    @Column(nullable = false)
    private String alertName;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String errorSignature;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> commands;

    @Column(columnDefinition = "TEXT")
    private String resolutionContext;

    @Column(nullable = false)
    private int successCount;

    @Column(name = "is_valid", nullable = false)
    private boolean isValid;

    @Builder
    public AlertRunbook(String alertName, String errorSignature, List<String> commands, String resolutionContext) {
        this.alertName = alertName;
        this.errorSignature = errorSignature;
        this.commands = commands;
        this.resolutionContext = resolutionContext;
        this.successCount = 0;
        this.isValid = true;
    }

    public void incrementSuccess() {
        this.successCount++;
    }

    public void invalidate() {
        this.isValid = false;
    }
}
