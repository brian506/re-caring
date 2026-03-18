package com.recaring.auth.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QLoginHistory is a Querydsl query type for LoginHistory
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLoginHistory extends EntityPathBase<LoginHistory> {

    private static final long serialVersionUID = 2137966908L;

    public static final QLoginHistory loginHistory = new QLoginHistory("loginHistory");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ipAddress = createString("ipAddress");

    public final BooleanPath isSuccess = createBoolean("isSuccess");

    public final StringPath memberKey = createString("memberKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath userAgent = createString("userAgent");

    public QLoginHistory(String variable) {
        super(LoginHistory.class, forVariable(variable));
    }

    public QLoginHistory(Path<? extends LoginHistory> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLoginHistory(PathMetadata metadata) {
        super(LoginHistory.class, metadata);
    }

}

