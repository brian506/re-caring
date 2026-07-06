package com.recaring.safezone.implement;

import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import com.recaring.safezone.vo.SafeZoneCreation;
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
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
@DisplayName("SafeZoneWriter 단위 테스트")
class SafeZoneWriterTest {

    @InjectMocks
    private SafeZoneWriter safeZoneWriter;

    @Mock
    private SafeZoneRepository safeZoneRepository;

    @Test
    @DisplayName("register 호출 시 올바른 필드로 안심존 엔티티를 저장한다")
    void register_saves_entity_with_correct_fields() {
        SafeZoneCreation command = SafeZoneFixture.createCommand();
        ArgumentCaptor<SafeZone> captor = ArgumentCaptor.forClass(SafeZone.class);

        safeZoneWriter.register(command);

        then(safeZoneRepository).should(times(1)).save(captor.capture());
        SafeZone saved = captor.getValue();
        assertThat(saved.getWardMemberKey()).isEqualTo(SafeZoneFixture.WARD_MEMBER_KEY);
        assertThat(saved.getName()).isEqualTo(SafeZoneFixture.NAME);
        assertThat(saved.getAddress()).isEqualTo(SafeZoneFixture.ADDRESS);
        assertThat(saved.getRadius()).isEqualTo(SafeZoneFixture.RADIUS);
        assertThat(saved.getSafeZoneKey()).isNotBlank();
    }

    @Test
    @DisplayName("update 호출 시 조회한 엔티티 필드가 command 값으로 변경된다")
    void update_mutates_entity() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        SafeZoneUpdate command = SafeZoneFixture.updateCommand();
        given(safeZoneRepository.findBySafeZoneKey(zone.getSafeZoneKey())).willReturn(Optional.of(zone));

        safeZoneWriter.update(zone.getSafeZoneKey(), command);

        assertThat(zone.getName()).isEqualTo(SafeZoneFixture.UPDATED_NAME);
        assertThat(zone.getAddress()).isEqualTo(SafeZoneFixture.UPDATED_ADDRESS);
        assertThat(zone.getRadius()).isEqualTo(SafeZoneFixture.UPDATED_RADIUS);
    }

    @Test
    @DisplayName("update 시 존재하지 않는 safeZoneKey면 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void update_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKey("unknown-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneWriter.update("unknown-key", SafeZoneFixture.updateCommand()))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
    }

    @Test
    @DisplayName("delete 호출 시 조회한 엔티티를 repository.delete()로 hard-delete 한다")
    void delete_hard_deletes_entity() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        given(safeZoneRepository.findBySafeZoneKey(zone.getSafeZoneKey())).willReturn(Optional.of(zone));

        safeZoneWriter.delete(zone.getSafeZoneKey());

        then(safeZoneRepository).should(times(1)).delete(zone);
    }

    @Test
    @DisplayName("delete 시 존재하지 않는 safeZoneKey면 NOT_FOUND_SAFE_ZONE 예외가 발생한다")
    void delete_throws_when_not_found() {
        given(safeZoneRepository.findBySafeZoneKey("unknown-key")).willReturn(Optional.empty());

        assertThatThrownBy(() -> safeZoneWriter.delete("unknown-key"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorType", ErrorType.NOT_FOUND_SAFE_ZONE);
        then(safeZoneRepository).should(times(0)).delete(any());
    }
}
