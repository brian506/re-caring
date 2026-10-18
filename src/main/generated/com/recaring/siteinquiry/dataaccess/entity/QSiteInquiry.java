package com.recaring.siteinquiry.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QSiteInquiry is a Querydsl query type for SiteInquiry
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSiteInquiry extends EntityPathBase<SiteInquiry> {

    private static final long serialVersionUID = -573947449L;

    public static final QSiteInquiry siteInquiry = new QSiteInquiry("siteInquiry");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath answer = createString("answer");

    public final DateTimePath<java.time.LocalDateTime> answeredAt = createDateTime("answeredAt", java.time.LocalDateTime.class);

    public final EnumPath<SiteInquiryCategory> category = createEnum("category", SiteInquiryCategory.class);

    public final StringPath content = createString("content");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final StringPath email = createString("email");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final StringPath inquiryKey = createString("inquiryKey");

    public final StringPath passwordHash = createString("passwordHash");

    public final StringPath phone = createString("phone");

    public final BooleanPath secret = createBoolean("secret");

    public final EnumPath<SiteInquiryStatus> status = createEnum("status", SiteInquiryStatus.class);

    public final StringPath title = createString("title");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QSiteInquiry(String variable) {
        super(SiteInquiry.class, forVariable(variable));
    }

    public QSiteInquiry(Path<? extends SiteInquiry> path) {
        super(path.getType(), path.getMetadata());
    }

    public QSiteInquiry(PathMetadata metadata) {
        super(SiteInquiry.class, metadata);
    }

}

