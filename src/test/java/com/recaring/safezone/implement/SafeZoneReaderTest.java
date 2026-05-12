package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.vo.SafeZoneInfo;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneReader 단위 테스트")
class SafeZoneReaderTest {

    @InjectMocks
    private SafeZoneReader safeZoneReader;

    @Mock
    private SafeZoneRepository safeZoneRepository;

    @Test
    @DisplayName("wardMemberKey로 안심존 목록을 VO로 변환해 반환한다")
    void findAllByWardMemberKey_returns_mapped_list() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(safeZoneRepository.findAllByWardMemberKey(SafeZoneFixture.WARD_MEMBER_KEY))
                .willReturn(List.of(zone));

        List<SafeZoneInfo> result = safeZoneReader.findAllByWardMemberKey(SafeZoneFixture.WARD_MEMBER_KEY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo(SafeZoneFixture.NAME);
        assertThat(result.get(0).radius()).isEqualTo(SafeZoneFixture.RADIUS);
        then(safeZoneRepository).should(times(1)).findAllByWardMemberKey(SafeZoneFixture.WARD_MEMBER_KEY);
    }

    @Test
    @DisplayName("safeZoneKey로 안심존 VO를 반환한다")
    void findBySafeZoneKey_returns_info() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(safeZoneRepository.findBySafeZoneKey(zone.getSafeZoneKey()))
                .willReturn(Optional.of(zone));

        SafeZoneInfo result = safeZoneReader.findBySafeZoneKey(zone.getSafeZoneKey());

        assertThat(result.name()).isEqualTo(SafeZoneFixture.NAME);
        assertThat(result.address()).isEqualTo(SafeZoneFixture.ADDRESS);
    }

    @Test
    @DisplayName("존재하지 않는 safeZoneKey 조회 시 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void findBySafeZoneKey_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKey("unknown-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneReader.findBySafeZoneKey("unknown-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
    }

    @Test
    @DisplayName("getEntity는 수정/삭제용 엔티티를 반환한다")
    void getEntity_returns_entity() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(safeZoneRepository.findBySafeZoneKey(zone.getSafeZoneKey()))
                .willReturn(Optional.of(zone));

        SafeZone result = safeZoneReader.getEntity(zone.getSafeZoneKey());

        assertThat(result).isEqualTo(zone);
    }

    @Test
    @DisplayName("getEntity 조회 시 존재하지 않으면 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void getEntity_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKey("unknown-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneReader.getEntity("unknown-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
    }
}
