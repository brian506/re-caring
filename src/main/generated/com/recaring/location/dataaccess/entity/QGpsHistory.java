package com.recaring.location.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QGpsHistory is a Querydsl query type for GpsHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QGpsHistory extends EntityPathBase<GpsHistory> {

    private static final long serialVersionUID = -113847762L;

    public static final QGpsHistory gpsHistory = new QGpsHistory("gpsHistory");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final DateTimePath<java.time.LocalDateTime> recordedAt = createDateTime("recordedAt", java.time.LocalDateTime.class);

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QGpsHistory(String variable) {
        super(GpsHistory.class, forVariable(variable));
    }

    public QGpsHistory(Path<? extends GpsHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QGpsHistory(PathMetadata metadata) {
        super(GpsHistory.class, metadata);
    }

}

