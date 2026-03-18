package com.recaring.auth.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QLocalAuth is a Querydsl query type for LocalAuth
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLocalAuth extends EntityPathBase<LocalAuth> {

    private static final long serialVersionUID = -86664958L;

    public static final QLocalAuth localAuth = new QLocalAuth("localAuth");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memberKey = createString("memberKey");

    public final StringPath password = createString("password");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QLocalAuth(String variable) {
        super(LocalAuth.class, forVariable(variable));
    }

    public QLocalAuth(Path<? extends LocalAuth> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLocalAuth(PathMetadata metadata) {
        super(LocalAuth.class, metadata);
    }

}

