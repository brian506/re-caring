package com.recaring.auth.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QRefreshToken is a Querydsl query type for RefreshToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRefreshToken extends EntityPathBase<RefreshToken> {

    private static final long serialVersionUID = 1461550127L;

    public static final QRefreshToken refreshToken = new QRefreshToken("refreshToken");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> expiredAt = createDateTime("expiredAt", java.time.LocalDateTime.class);

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memberKey = createString("memberKey");

    public final StringPath token = createString("token");

    public QRefreshToken(String variable) {
        super(RefreshToken.class, forVariable(variable));
    }

    public QRefreshToken(Path<? extends RefreshToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QRefreshToken(PathMetadata metadata) {
        super(RefreshToken.class, metadata);
    }

}

