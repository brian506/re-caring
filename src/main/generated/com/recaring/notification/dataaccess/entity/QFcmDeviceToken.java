package com.recaring.notification.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QFcmDeviceToken is a Querydsl query type for FcmDeviceToken
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFcmDeviceToken extends EntityPathBase<FcmDeviceToken> {

    private static final long serialVersionUID = -1931302655L;

    public static final QFcmDeviceToken fcmDeviceToken = new QFcmDeviceToken("fcmDeviceToken");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final BooleanPath active = createBoolean("active");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final DateTimePath<java.time.LocalDateTime> deactivatedAt = createDateTime("deactivatedAt", java.time.LocalDateTime.class);

    public final StringPath deactivationReason = createString("deactivationReason");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> lastUsedAt = createDateTime("lastUsedAt", java.time.LocalDateTime.class);

    public final StringPath memberKey = createString("memberKey");

    public final EnumPath<FcmDevicePlatform> platform = createEnum("platform", FcmDevicePlatform.class);

    public final EnumPath<NotificationRecipientType> recipientType = createEnum("recipientType", NotificationRecipientType.class);

    public final StringPath token = createString("token");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QFcmDeviceToken(String variable) {
        super(FcmDeviceToken.class, forVariable(variable));
    }

    public QFcmDeviceToken(Path<? extends FcmDeviceToken> path) {
        super(path.getType(), path.getMetadata());
    }

    public QFcmDeviceToken(PathMetadata metadata) {
        super(FcmDeviceToken.class, metadata);
    }

}

