package com.recaring.safezone.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QSafeZone is a Querydsl query type for SafeZone
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSafeZone extends EntityPathBase<SafeZone> {

    private static final long serialVersionUID = 1339573241L;

    public static final QSafeZone safeZone = new QSafeZone("safeZone");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath address = createString("address");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Double> latitude = createNumber("latitude", Double.class);

    public final NumberPath<Double> longitude = createNumber("longitude", Double.class);

    public final StringPath name = createString("name");

    public final NumberPath<Integer> radiusMeters = createNumber("radiusMeters", Integer.class);

    public final StringPath safeZoneKey = createString("safeZoneKey");

    public final EnumPath<com.recaring.safezone.vo.SafeZoneType> type = createEnum("type", com.recaring.safezone.vo.SafeZoneType.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QSafeZone(String variable) {
        super(SafeZone.class, forVariable(variable));
    }

    public QSafeZone(Path<? extends SafeZone> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSafeZone(PathMetadata metadata) {
        super(SafeZone.class, metadata);
    }

}

