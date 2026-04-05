package com.recaring.care.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QCareInvitation is a Querydsl query type for CareInvitation
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCareInvitation extends EntityPathBase<CareInvitation> {

    private static final long serialVersionUID = -645905070L;

    public static final QCareInvitation careInvitation = new QCareInvitation("careInvitation");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final EnumPath<CareRole> careRole = createEnum("careRole", CareRole.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath requesterMemberKey = createString("requesterMemberKey");

    public final StringPath requestKey = createString("requestKey");

    public final EnumPath<CareInvitationStatus> status = createEnum("status", CareInvitationStatus.class);

    public final StringPath targetMemberKey = createString("targetMemberKey");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QCareInvitation(String variable) {
        super(CareInvitation.class, forVariable(variable));
    }

    public QCareInvitation(Path<? extends CareInvitation> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCareInvitation(PathMetadata metadata) {
        super(CareInvitation.class, metadata);
    }

}

