package com.recaring.location.implement.safezone;

import com.recaring.location.dataaccess.entity.SafeZoneState;
import com.recaring.location.dataaccess.repository.SafeZoneStateRepository;
import com.recaring.location.vo.SafeZoneObservation;
import com.recaring.location.vo.SafeZoneTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * WARD가 현재 어느 안심존 안에 있는지를 저장한다. 진입·이탈은 이 직전 상태와의 차이로만 판정하므로,
 * 존 안에 머무는 동안 같은 알림이 반복되지 않는다.
 * 행이 아예 없는 경우(최초 관측)와 존 밖에 있는 경우(빈 집합)를 구분해야 하므로 Optional로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeZoneStateManager {

    private static final String DELIMITER = ",";

    private final SafeZoneStateRepository safeZoneStateRepository;

    /**
     * 직전 상태를 기준으로 현재 상태를 정해 저장하고, 그 전이를 반환한다. 저장이 실패하면 트랜잭션이 롤백되어
     * 호출부가 알림을 발행하지 않으므로, 저장 없이 알림만 나가는 상태가 생기지 않는다.
     */
    @Transactional
    public Optional<SafeZoneTransition> transition(String wardMemberKey, SafeZoneObservation observation) {
        Optional<SafeZoneState> found = safeZoneStateRepository.findByWardMemberKey(wardMemberKey);
        if (found.isEmpty()) {
            safeZoneStateRepository.save(SafeZoneState.builder()
                    .wardMemberKey(wardMemberKey)
                    .safeZoneKeys(join(observation.insideKeys()))
                    .build());
            return Optional.empty();
        }

        SafeZoneState state = found.get();
        Set<String> previousKeys = parse(state.getSafeZoneKeys());
        Set<String> currentKeys = observation.resolve(previousKeys);
        state.replace(join(currentKeys));
        return Optional.of(new SafeZoneTransition(previousKeys, currentKeys));
    }

    @Transactional
    public void delete(String wardMemberKey) {
        safeZoneStateRepository.deleteByWardMemberKey(wardMemberKey);
    }

    private String join(Set<String> safeZoneKeys) {
        return String.join(DELIMITER, safeZoneKeys);
    }

    private Set<String> parse(String value) {
        if (value.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(value.split(DELIMITER)));
    }
}
