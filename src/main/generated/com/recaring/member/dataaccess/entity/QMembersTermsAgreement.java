package com.recaring.member.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QMembersTermsAgreement is a Querydsl query type for MembersTermsAgreement
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QMembersTermsAgreement extends EntityPathBase<MembersTermsAgreement> {

    private static final long serialVersionUID = 1946642301L;

    public static final QMembersTermsAgreement membersTermsAgreement = new QMembersTermsAgreement("membersTermsAgreement");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath memberKey = createString("memberKey");

    public final DateTimePath<java.time.LocalDateTime> termsLocationAgreedAt = createDateTime("termsLocationAgreedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> termsPrivacyAgreedAt = createDateTime("termsPrivacyAgreedAt", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> termsServiceAgreedAt = createDateTime("termsServiceAgreedAt", java.time.LocalDateTime.class);

    public QMembersTermsAgreement(String variable) {
        super(MembersTermsAgreement.class, forVariable(variable));
    }

    public QMembersTermsAgreement(Path<? extends MembersTermsAgreement> path) {
        super(path.getType(), path.getMetadata());
    }

    public QMembersTermsAgreement(PathMetadata metadata) {
        super(MembersTermsAgreement.class, metadata);
    }

}

