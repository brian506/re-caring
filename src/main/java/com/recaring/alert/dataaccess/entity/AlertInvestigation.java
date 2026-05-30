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

// TODO: CREATE INDEX idx_alert_investigations_fingerprint ON alert_investigations(fingerprint);
// TODO: CREATE INDEX idx_alert_investigations_status ON alert_investigations(status) WHERE deleted_at IS NULL;
@Getter
@Entity
@Table(name = "alert_investigations")
@SQLDelete(sql = "UPDATE alert_investigations SET deleted_at = NOW() WHERE alert_investigation_id = ?")
@SQLRestriction("deleted_at IS NULL")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlertInvestigation extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_investigation_id")
    private Long id;

    @Column(nullable = false)
    private String fingerprint;

    @Column(nullable = false)
    private String alertName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InvestigationStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "fix_commands", nullable = false, columnDefinition = "jsonb")
    private List<String> fixCommands;

    @Column(columnDefinition = "TEXT")
    private String claudeAnalysis;

    @Builder
    public AlertInvestigation(String fingerprint, String alertName, AlertSeverity severity) {
        this.fingerprint = fingerprint;
        this.alertName = alertName;
        this.severity = severity;
        this.status = InvestigationStatus.PENDING;
        this.fixCommands = List.of();
    }

    public void resolve(List<String> fixCommands, String claudeAnalysis) {
        this.status = InvestigationStatus.RESOLVED;
        this.fixCommands = fixCommands;
        this.claudeAnalysis = claudeAnalysis;
    }

    public void fail(String claudeAnalysis) {
        this.status = InvestigationStatus.FAILED;
        this.claudeAnalysis = claudeAnalysis;
    }

}
