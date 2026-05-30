package com.recaring.alert.implement;

import com.recaring.alert.vo.AlertItem;
import com.recaring.alert.vo.GpsRecoveryResult;
import com.recaring.alert.vo.GpsVerdict;

public interface SlackAlertNotifier {

    String sendInitialAlert(AlertItem alert);

    void sendGpsResolution(String threadTs, GpsVerdict verdict, GpsRecoveryResult recovery);

    void sendError(String threadTs, String message);
}
