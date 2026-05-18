package com.recaring.location.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QLocationSetting is a Querydsl query type for LocationSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLocationSetting extends EntityPathBase<LocationSetting> {

    private static final long serialVersionUID = -1029427785L;

    public static final QLocationSetting locationSetting = new QLocationSetting("locationSetting");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final NumberPath<Integer> collectionIntervalSeconds = createNumber("collectionIntervalSeconds", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QLocationSetting(String variable) {
        super(LocationSetting.class, forVariable(variable));
    }

    public QLocationSetting(Path<? extends LocationSetting> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLocationSetting(PathMetadata metadata) {
        super(LocationSetting.class, metadata);
    }

}

