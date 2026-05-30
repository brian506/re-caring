package com.recaring.alert.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlertInvestigation is a Querydsl query type for AlertInvestigation
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlertInvestigation extends EntityPathBase<AlertInvestigation> {

    private static final long serialVersionUID = 1055454371L;

    public static final QAlertInvestigation alertInvestigation = new QAlertInvestigation("alertInvestigation");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath alertName = createString("alertName");

    public final StringPath claudeAnalysis = createString("claudeAnalysis");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath fingerprint = createString("fingerprint");

    public final ListPath<String, StringPath> fixCommands = this.<String, StringPath>createList("fixCommands", String.class, StringPath.class, PathInits.DIRECT2);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final EnumPath<AlertSeverity> severity = createEnum("severity", AlertSeverity.class);

    public final EnumPath<InvestigationStatus> status = createEnum("status", InvestigationStatus.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAlertInvestigation(String variable) {
        super(AlertInvestigation.class, forVariable(variable));
    }

    public QAlertInvestigation(Path<? extends AlertInvestigation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAlertInvestigation(PathMetadata metadata) {
        super(AlertInvestigation.class, metadata);
    }

}

