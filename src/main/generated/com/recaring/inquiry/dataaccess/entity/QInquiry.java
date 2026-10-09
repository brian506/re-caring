package com.recaring.inquiry.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QInquiry is a Querydsl query type for Inquiry
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QInquiry extends EntityPathBase<Inquiry> {

    private static final long serialVersionUID = -550372107L;

    public static final QInquiry inquiry = new QInquiry("inquiry");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath answer = createString("answer");

    public final DateTimePath<java.time.LocalDateTime> answeredAt = createDateTime("answeredAt", java.time.LocalDateTime.class);

    public final EnumPath<InquiryCategory> category = createEnum("category", InquiryCategory.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath inquiryKey = createString("inquiryKey");

    public final StringPath memberKey = createString("memberKey");

    public final EnumPath<InquiryStatus> status = createEnum("status", InquiryStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QInquiry(String variable) {
        super(Inquiry.class, forVariable(variable));
    }

    public QInquiry(Path<? extends Inquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QInquiry(PathMetadata metadata) {
        super(Inquiry.class, metadata);
    }

}

