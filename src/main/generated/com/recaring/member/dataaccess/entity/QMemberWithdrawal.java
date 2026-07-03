package com.recaring.member.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QMemberWithdrawal is a Querydsl query type for MemberWithdrawal
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMemberWithdrawal extends EntityPathBase<MemberWithdrawal> {

    private static final long serialVersionUID = -211037042L;

    public static final QMemberWithdrawal memberWithdrawal = new QMemberWithdrawal("memberWithdrawal");

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memberKey = createString("memberKey");

    public final EnumPath<MemberRole> role = createEnum("role", MemberRole.class);

    public final DateTimePath<java.time.LocalDateTime> withdrawnAt = createDateTime("withdrawnAt", java.time.LocalDateTime.class);

    public QMemberWithdrawal(String variable) {
        super(MemberWithdrawal.class, forVariable(variable));
    }

    public QMemberWithdrawal(Path<? extends MemberWithdrawal> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMemberWithdrawal(PathMetadata metadata) {
        super(MemberWithdrawal.class, metadata);
    }

}

