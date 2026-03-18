package com.recaring.auth.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QOAuth is a Querydsl query type for OAuth
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QOAuth extends EntityPathBase<OAuth> {

    private static final long serialVersionUID = -936077434L;

    public static final QOAuth oAuth = new QOAuth("oAuth");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memberKey = createString("memberKey");

    public final EnumPath<com.recaring.auth.vo.OAuthProvider> provider = createEnum("provider", com.recaring.auth.vo.OAuthProvider.class);

    public final StringPath providerUserId = createString("providerUserId");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QOAuth(String variable) {
        super(OAuth.class, forVariable(variable));
    }

    public QOAuth(Path<? extends OAuth> path) {
        super(path.getType(), path.getMetadata());
    }

    public QOAuth(PathMetadata metadata) {
        super(OAuth.class, metadata);
    }

}

