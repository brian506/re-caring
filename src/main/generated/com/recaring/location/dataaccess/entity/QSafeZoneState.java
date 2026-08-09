package com.recaring.location.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QSafeZoneState is a Querydsl query type for SafeZoneState
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSafeZoneState extends EntityPathBase<SafeZoneState> {

    private static final long serialVersionUID = -1974305004L;

    public static final QSafeZoneState safeZoneState = new QSafeZoneState("safeZoneState");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath safeZoneKeys = createString("safeZoneKeys");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QSafeZoneState(String variable) {
        super(SafeZoneState.class, forVariable(variable));
    }

    public QSafeZoneState(Path<? extends SafeZoneState> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSafeZoneState(PathMetadata metadata) {
        super(SafeZoneState.class, metadata);
    }

}

