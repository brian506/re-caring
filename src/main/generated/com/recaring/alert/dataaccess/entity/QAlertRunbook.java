package com.recaring.alert.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAlertRunbook is a Querydsl query type for AlertRunbook
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAlertRunbook extends EntityPathBase<AlertRunbook> {

    private static final long serialVersionUID = -325511595L;

    public static final QAlertRunbook alertRunbook = new QAlertRunbook("alertRunbook");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final StringPath alertName = createString("alertName");

    public final ListPath<String, StringPath> commands = this.<String, StringPath>createList("commands", String.class, StringPath.class, PathInits.DIRECT2);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath errorSignature = createString("errorSignature");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath isValid = createBoolean("isValid");

    public final StringPath resolutionContext = createString("resolutionContext");

    public final NumberPath<Integer> successCount = createNumber("successCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QAlertRunbook(String variable) {
        super(AlertRunbook.class, forVariable(variable));
    }

    public QAlertRunbook(Path<? extends AlertRunbook> path) {
        super(path.getType(), path.getMetadata());
    }

    public QAlertRunbook(PathMetadata metadata) {
        super(AlertRunbook.class, metadata);
    }

}

