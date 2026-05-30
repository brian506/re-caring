package com.recaring.alert.implement;

public interface SsmExecutor {

    String executeReadOnly(String command);

    String executeFix(String command);
}
