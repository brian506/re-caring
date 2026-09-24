package com.recaring.notification.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotificationFeedback is a Querydsl query type for NotificationFeedback
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationFeedback extends EntityPathBase<NotificationFeedback> {

    private static final long serialVersionUID = -925552578L;

    public static final QNotificationFeedback notificationFeedback = new QNotificationFeedback("notificationFeedback");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final EnumPath<FeedbackAccuracy> accuracy = createEnum("accuracy", FeedbackAccuracy.class);

    public final NumberPath<Long> anomalyDetectionId = createNumber("anomalyDetectionId", Long.class);

    public final StringPath comment = createString("comment");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final NumberPath<Long> notificationId = createNumber("notificationId", Long.class);

    public final EnumPath<FeedbackReason> reason = createEnum("reason", FeedbackReason.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public QNotificationFeedback(String variable) {
        super(NotificationFeedback.class, forVariable(variable));
    }

    public QNotificationFeedback(Path<? extends NotificationFeedback> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotificationFeedback(PathMetadata metadata) {
        super(NotificationFeedback.class, metadata);
    }

}

