package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.vo.SafeZoneUpdate;
import com.recaring.support.exception.AppException;
import com.recaring.support.exception.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneWriter 단위 테스트")
class SafeZoneWriterTest {

    private static final String UNKNOWN_KEY = SafeZoneFixture.UNKNOWN_SAFE_ZONE_KEY;

    @InjectMocks
    private SafeZoneWriter safeZoneWriter;

    @Mock
    private SafeZoneRepository safeZoneRepository;

    @Test
    @DisplayName("등록 요청의 좌표·반경이 뒤바뀌지 않고 그대로 안심존으로 저장된다")
    void register_saves_entity_with_command_values() {
        ArgumentCaptor<SafeZone> captor = ArgumentCaptor.forClass(SafeZone.class);

        safeZoneWriter.register(SafeZoneFixture.createCommand());

        then(safeZoneRepository).should().save(captor.capture());
        SafeZone saved = captor.getValue();
        assertThat(saved.getWardMemberKey()).isEqualTo(SafeZoneFixture.WARD_MEMBER_KEY);
        assertThat(saved.getName()).isEqualTo(SafeZoneFixture.NAME);
        assertThat(saved.getAddress()).isEqualTo(SafeZoneFixture.ADDRESS);
        assertThat(saved.getLatitude()).isEqualTo(SafeZoneFixture.LATITUDE);
        assertThat(saved.getLongitude()).isEqualTo(SafeZoneFixture.LONGITUDE);
        assertThat(saved.getRadius()).isEqualTo(SafeZoneFixture.RADIUS);
        assertThat(saved.getSafeZoneKey()).isNotBlank();
    }

    @Test
    @DisplayName("수정 요청의 좌표·반경이 뒤바뀌지 않고 기존 안심존에 반영된다")
    void update_applies_command_values_to_existing_zone() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        SafeZoneUpdate command = SafeZoneFixture.updateCommand();
        given(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(zone.getSafeZoneKey(), zone.getWardMemberKey())).willReturn(Optional.of(zone));

        safeZoneWriter.update(zone.getSafeZoneKey(), zone.getWardMemberKey(), command);

        assertThat(zone.getName()).isEqualTo(SafeZoneFixture.UPDATED_NAME);
        assertThat(zone.getAddress()).isEqualTo(SafeZoneFixture.UPDATED_ADDRESS);
        assertThat(zone.getLatitude()).isEqualTo(SafeZoneFixture.UPDATED_LATITUDE);
        assertThat(zone.getLongitude()).isEqualTo(SafeZoneFixture.UPDATED_LONGITUDE);
        assertThat(zone.getRadius()).isEqualTo(SafeZoneFixture.UPDATED_RADIUS);
    }

    @Test
    @DisplayName("존재하지 않는 안심존은 수정할 수 없고 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void update_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(UNKNOWN_KEY, SafeZoneFixture.WARD_MEMBER_KEY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneWriter.update(UNKNOWN_KEY, SafeZoneFixture.WARD_MEMBER_KEY, SafeZoneFixture.updateCommand()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
    }

    @Test
    @DisplayName("삭제는 조회한 안심존을 hard-delete 한다")
    void delete_hard_deletes_entity() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(zone.getSafeZoneKey(), zone.getWardMemberKey())).willReturn(Optional.of(zone));

        safeZoneWriter.delete(zone.getSafeZoneKey(), zone.getWardMemberKey());

        then(safeZoneRepository).should().delete(zone);
    }

    @Test
    @DisplayName("존재하지 않는 안심존은 삭제하지 않고 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void delete_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKeyAndWardMemberKey(UNKNOWN_KEY, SafeZoneFixture.WARD_MEMBER_KEY)).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneWriter.delete(UNKNOWN_KEY, SafeZoneFixture.WARD_MEMBER_KEY))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
        then(safeZoneRepository).should(never()).delete(any());
    }
}
