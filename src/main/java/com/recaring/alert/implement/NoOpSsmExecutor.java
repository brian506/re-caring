package com.recaring.alert.implement;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"local", "test"})
public class NoOpSsmExecutor implements SsmExecutor {

    @Override
    public String executeReadOnly(String command) {
        log.info("[SSM 실행 : NO-OP 읽기]: command={}", command);
        return "NO-OP: local profile — command=" + command;
    }

    @Override
    public String executeFix(String command) {
        log.info("[SSM 실행 : NO-OP 수정]: command={}", command);
        return "NO-OP: local profile — command=" + command;
    }
}
