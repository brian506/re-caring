package com.recaring.safezone.implement;

import com.recaring.safezone.controller.request.CreateSafeZoneCommand;
import com.recaring.safezone.controller.request.UpdateSafeZoneCommand;
import com.recaring.safezone.dataaccess.entity.SafeZone;
import com.recaring.safezone.dataaccess.repository.SafeZoneRepository;
import com.recaring.safezone.fixture.SafeZoneFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
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
        CreateSafeZoneCommand command = SafeZoneFixture.createCommand();
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
    @DisplayName("update 호출 시 엔티티 필드가 command 값으로 변경되고 저장된다")
    void update_mutates_entity_and_saves() {
        SafeZone zone = SafeZoneFixture.createSafeZone();
        UpdateSafeZoneCommand command = SafeZoneFixture.updateCommand();

        safeZoneWriter.update(zone, command);

        assertThat(zone.getName()).isEqualTo(SafeZoneFixture.UPDATED_NAME);
        assertThat(zone.getAddress()).isEqualTo(SafeZoneFixture.UPDATED_ADDRESS);
        assertThat(zone.getRadius()).isEqualTo(SafeZoneFixture.UPDATED_RADIUS);
        then(safeZoneRepository).should(times(1)).save(zone);
    }

    @Test
    @DisplayName("delete 호출 시 deletedAt이 설정되고 저장된다")
    void delete_marks_deleted_and_saves() {
        SafeZone zone = SafeZoneFixture.createSafeZone();

        safeZoneWriter.delete(zone);

        assertThat(zone.isDeleted()).isTrue();
        then(safeZoneRepository).should(times(1)).save(zone);
    }
}
