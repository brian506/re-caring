# PR 본문 템플릿

```markdown
## 개요
{구현한 기능 또는 수정 내용을 1~2문장으로 설명}

## 변경 사항
- {주요 변경 사항 1}
- {주요 변경 사항 2}
- {주요 변경 사항 3}

## 관련 이슈
closes #{이슈번호}

## 테스트
- [ ] 단위 테스트 통과 (`./gradlew test`)
- [ ] 전체 빌드 성공 (`./gradlew build`)

## 참고
{관련 문서, 참고 링크 등 (없으면 생략)}
```

---

## 작성 예시

```markdown
## 개요
회원이 서비스를 탈퇴할 수 있는 API를 구현했습니다.
소프트 딜리트 전략을 적용하며 탈퇴 시 RefreshToken도 함께 삭제합니다.

## 변경 사항
- `DELETE /api/v1/members/me` 엔드포인트 추가
- `MemberWriter.remove()` 소프트 딜리트 구현
- `RefreshTokenWriter.removeByMemberKey()` 토큰 삭제 구현
- `MemberService.withdraw()` 단위 테스트 추가

## 관련 이슈
closes #42

## 테스트
- [x] 단위 테스트 통과 (`./gradlew test`)
- [x] 전체 빌드 성공 (`./gradlew build`)
```
