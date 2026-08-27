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

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> detectedAt = createDateTime("detectedAt", java.time.LocalDateTime.class);

    public final EnumPath<com.recaring.location.vo.DetectionType> detectionType = createEnum("detectionType", com.recaring.location.vo.DetectionType.class);

    public final StringPath evidence = createString("evidence");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final NumberPath<Double> score = createNumber("score", Double.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

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

