package com.recaring.notification.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationDelivery is a Querydsl query type for NotificationDelivery
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationDelivery extends EntityPathBase<NotificationDelivery> {

    private static final long serialVersionUID = 89415853L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationDelivery notificationDelivery = new QNotificationDelivery("notificationDelivery");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final NumberPath<Integer> attemptCount = createNumber("attemptCount", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.LocalDateTime> deletedAt = _super.deletedAt;

    public final StringPath failureCode = createString("failureCode");

    public final StringPath failureReason = createString("failureReason");

    public final NumberPath<Long> fcmDeviceTokenId = createNumber("fcmDeviceTokenId", Long.class);

    public final StringPath fcmMessageId = createString("fcmMessageId");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final DateTimePath<java.time.LocalDateTime> nextRetryAt = createDateTime("nextRetryAt", java.time.LocalDateTime.class);

    public final QNotification notification;

    public final StringPath recipientMemberKey = createString("recipientMemberKey");

    public final EnumPath<NotificationRecipientType> recipientType = createEnum("recipientType", NotificationRecipientType.class);

    public final BooleanPath retryable = createBoolean("retryable");

    public final DateTimePath<java.time.LocalDateTime> sentAt = createDateTime("sentAt", java.time.LocalDateTime.class);

    public final EnumPath<NotificationDeliveryStatus> status = createEnum("status", NotificationDeliveryStatus.class);

    public final StringPath tokenSnapshot = createString("tokenSnapshot");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationDelivery(String variable) {
        this(NotificationDelivery.class, forVariable(variable), INITS);
    }

    public QNotificationDelivery(Path<? extends NotificationDelivery> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationDelivery(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationDelivery(PathMetadata metadata, PathInits inits) {
        this(NotificationDelivery.class, metadata, inits);
    }

    public QNotificationDelivery(Class<? extends NotificationDelivery> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.notification = inits.isInitialized("notification") ? new QNotification(forProperty("notification")) : null;
    }

}

