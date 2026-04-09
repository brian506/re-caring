package com.recaring.care.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QCareRelationship is a Querydsl query type for CareRelationship
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QCareRelationship extends EntityPathBase<CareRelationship> {

    private static final long serialVersionUID = -173491791L;

    public static final QCareRelationship careRelationship = new QCareRelationship("careRelationship");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath caregiverMemberKey = createString("caregiverMemberKey");

    public final EnumPath<CareRole> careRole = createEnum("careRole", CareRole.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QCareRelationship(String variable) {
        super(CareRelationship.class, forVariable(variable));
    }

    public QCareRelationship(Path<? extends CareRelationship> path) {
        super(path.getType(), path.getMetadata());
    }

    public QCareRelationship(PathMetadata metadata) {
        super(CareRelationship.class, metadata);
    }

}

