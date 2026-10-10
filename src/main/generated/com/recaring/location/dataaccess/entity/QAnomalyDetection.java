package com.recaring.location.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QAnomalyDetection is a Querydsl query type for AnomalyDetection
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAnomalyDetection extends EntityPathBase<AnomalyDetection> {

    private static final long serialVersionUID = -63066778L;

    public static final QAnomalyDetection anomalyDetection = new QAnomalyDetection("anomalyDetection");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final EnumPath<com.recaring.location.vo.AnomalyAlertKind> alertKind = createEnum("alertKind", com.recaring.location.vo.AnomalyAlertKind.class);

    public final StringPath categories = createString("categories");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final EnumPath<com.recaring.location.vo.DetectionType> detectionType = createEnum("detectionType", com.recaring.location.vo.DetectionType.class);

    public final NumberPath<Integer> durationMin = createNumber("durationMin", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> eventEndedAt = createDateTime("eventEndedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> eventHour = createNumber("eventHour", Integer.class);

    public final StringPath eventId = createString("eventId");

    public final DateTimePath<java.time.LocalDateTime> eventStartedAt = createDateTime("eventStartedAt", java.time.LocalDateTime.class);

    public final StringPath evidence = createString("evidence");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final EnumPath<com.recaring.location.vo.AnomalyReasonCode> reasonCode = createEnum("reasonCode", com.recaring.location.vo.AnomalyReasonCode.class);

    public final DateTimePath<java.time.LocalDateTime> recordedAt = createDateTime("recordedAt", java.time.LocalDateTime.class);

    public final NumberPath<Integer> remindSeq = createNumber("remindSeq", Integer.class);

    public final NumberPath<Double> score = createNumber("score", Double.class);

    public final NumberPath<Integer> speedKmh = createNumber("speedKmh", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Integer> usualMin = createNumber("usualMin", Integer.class);

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QAnomalyDetection(String variable) {
        super(AnomalyDetection.class, forVariable(variable));
    }

    public QAnomalyDetection(Path<? extends AnomalyDetection> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAnomalyDetection(PathMetadata metadata) {
        super(AnomalyDetection.class, metadata);
    }

}

