package com.recaring.care.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QDesignatedAvatar is a Querydsl query type for DesignatedAvatar
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDesignatedAvatar extends EntityPathBase<DesignatedAvatar> {

    private static final long serialVersionUID = -850107887L;

    public static final QDesignatedAvatar designatedAvatar = new QDesignatedAvatar("designatedAvatar");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath ownerMemberKey = createString("ownerMemberKey");

    public final StringPath profileAvatarCode = createString("profileAvatarCode");

    public final StringPath targetMemberKey = createString("targetMemberKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QDesignatedAvatar(String variable) {
        super(DesignatedAvatar.class, forVariable(variable));
    }

    public QDesignatedAvatar(Path<? extends DesignatedAvatar> path) {
        super(path.getType(), path.getMetadata());
    }

    public QDesignatedAvatar(PathMetadata metadata) {
        super(DesignatedAvatar.class, metadata);
    }

}

