package com.recaring.device.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QWardDeviceToken is a Querydsl query type for WardDeviceToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWardDeviceToken extends EntityPathBase<WardDeviceToken> {

    private static final long serialVersionUID = 1214164068L;

    public static final QWardDeviceToken wardDeviceToken = new QWardDeviceToken("wardDeviceToken");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expiresAt = createDateTime("expiresAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath token = createString("token");

    public final StringPath wardKey = createString("wardKey");

    public QWardDeviceToken(String variable) {
        super(WardDeviceToken.class, forVariable(variable));
    }

    public QWardDeviceToken(Path<? extends WardDeviceToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWardDeviceToken(PathMetadata metadata) {
        super(WardDeviceToken.class, metadata);
    }

}

