package com.recaring.notification.dataaccess.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.dsl.StringTemplate;

import com.querydsl.core.types.PathMetadata;
import com.querydsl.core.annotations.Generated;
import com.querydsl.core.types.Path;


/**
 * QNotificationSetting is a Querydsl query type for NotificationSetting
 */
@SuppressWarnings("this-escape")
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationSetting extends EntityPathBase<NotificationSetting> {

    private static final long serialVersionUID = 2100809335L;

    public static final QNotificationSetting notificationSetting = new QNotificationSetting("notificationSetting");

    public final com.recaring.common.entity.QBaseEntity _super = new com.recaring.common.entity.QBaseEntity(this);

    public final EnumPath<com.recaring.notification.vo.AnomalySensitivity> anomalySensitivity = createEnum("anomalySensitivity", com.recaring.notification.vo.AnomalySensitivity.class);

    public final NumberPath<Integer> batteryThresholdPercent = createNumber("batteryThresholdPercent", Integer.class);

    //inherited
    public final DateTimePath<java.time.LocalDateTime> createdAt = _super.createdAt;

    public final BooleanPath emergencyCallEnabled = createBoolean("emergencyCallEnabled");

    public final NumberPath<Long> id = createNumber("id", Long.class);

    public final BooleanPath lowBatteryEnabled = createBoolean("lowBatteryEnabled");

    public final BooleanPath routeDeviationEnabled = createBoolean("routeDeviationEnabled");

    public final BooleanPath safeZoneEntryEnabled = createBoolean("safeZoneEntryEnabled");

    public final BooleanPath safeZoneExitEnabled = createBoolean("safeZoneExitEnabled");

    public final BooleanPath speedAnomalyEnabled = createBoolean("speedAnomalyEnabled");

    //inherited
    public final DateTimePath<java.time.LocalDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public final BooleanPath wanderingAnomalyEnabled = createBoolean("wanderingAnomalyEnabled");

    public final StringPath wardMemberKey = createString("wardMemberKey");

    public QNotificationSetting(String variable) {
        super(NotificationSetting.class, forVariable(variable));
    }

    public QNotificationSetting(Path<? extends NotificationSetting> path) {
        super(path.getType(), path.getMetadata());
    }

    public QNotificationSetting(PathMetadata metadata) {
        super(NotificationSetting.class, metadata);
    }

}

