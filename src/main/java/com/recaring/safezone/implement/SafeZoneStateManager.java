package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZoneState;
import com.recaring.safezone.dataaccess.repository.SafeZoneStateRepository;
import com.recaring.safezone.vo.SafeZoneObservation;
import com.recaring.safezone.vo.SafeZoneTransition;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * WARD가 현재 어느 안심존 안에 있는지를 저장한다. 진입·이탈은 이 직전 상태와의 차이로만 판정하므로,
 * 존 안에 머무는 동안 같은 알림이 반복되지 않는다.
 * 존이 새로 생기거나 수정되면 옛 설정으로 판정한 상태와 비교할 수 없으므로, 버전이 같은 존끼리만 비교하고
 * 나머지는 알림 없이 기준선을 다시 세운다.
 * 행이 아예 없는 경우(최초 관측)와 존 밖에 있는 경우(빈 집합)를 구분해야 하므로 Optional로 반환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SafeZoneStateManager {

    private static final String DELIMITER = ",";
    private static final String VERSION_DELIMITER = "@";

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
                    .zoneVersions(joinVersions(observation.zoneVersions()))
                    .build());
            return Optional.empty();
        }

        SafeZoneState state = found.get();
        Set<String> comparableKeys = observation.keysWithSameVersion(parseVersions(state.getZoneVersions()));
        Set<String> previousKeys = retain(parse(state.getSafeZoneKeys()), comparableKeys);
        Set<String> currentKeys = observation.resolve(previousKeys, comparableKeys);
        state.replace(join(currentKeys), joinVersions(observation.zoneVersions()));
        return Optional.of(new SafeZoneTransition(previousKeys, retain(currentKeys, comparableKeys)));
    }

    @Transactional
    public void delete(String wardMemberKey) {
        safeZoneStateRepository.deleteByWardMemberKey(wardMemberKey);
    }

    private String join(Set<String> safeZoneKeys) {
        return String.join(DELIMITER, safeZoneKeys);
    }

    private String joinVersions(Map<String, String> zoneVersions) {
        return zoneVersions.entrySet().stream()
                .map(entry -> entry.getKey() + VERSION_DELIMITER + entry.getValue())
                .collect(Collectors.joining(DELIMITER));
    }

    private Map<String, String> parseVersions(String value) {
        Map<String, String> versions = new LinkedHashMap<>();
        if (value == null || value.isBlank()) {
            return versions;
        }
        for (String entry : value.split(DELIMITER)) {
            String[] keyAndVersion = entry.split(VERSION_DELIMITER, 2);
            versions.put(keyAndVersion[0], keyAndVersion[1]);
        }
        return versions;
    }

    private Set<String> retain(Set<String> keys, Set<String> comparableKeys) {
        Set<String> retained = new LinkedHashSet<>(keys);
        retained.retainAll(comparableKeys);
        return retained;
    }

    private Set<String> parse(String value) {
        if (value.isBlank()) {
            return Set.of();
        }
        return new LinkedHashSet<>(Arrays.asList(value.split(DELIMITER)));
    }
}
