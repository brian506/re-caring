// realistic-data-gen.js
// 운영 분포(N ward × G guardian) 부하 테스트용 데이터 생성기.
//
// 같은 식별자로 두 산출물을 동시에 생성해 DB와 k6 데이터셋의 정합성을 보장한다.
//   - seed.sql      : DB에 직접 INSERT (members / care_relationships / ward_device_tokens)
//   - dataset.json  : k6 스크립트가 읽는 ward↔guardian 매핑 + device token + 사전 발급 JWT
//
// 사용:
//   JWT_KEY=<서버와 동일한 값> WARDS=700 GUARDIANS_PER_WARD=5 node realistic-data-gen.js
//
// JWT_KEY가 있으면 보호자 토큰을 미리 서명해서 dataset.json에 담는다.
// k6 스크립트는 런타임에 서명 없이 저장된 토큰을 그대로 사용한다.

const crypto = require('crypto');
const fs = require('fs');

const JWT_KEY = process.env.JWT_KEY || null;
const N = parseInt(process.env.WARDS || '300', 10);                 // 보호 대상자 수
const G = parseInt(process.env.GUARDIANS_PER_WARD || '5', 10);      // ward당 보호자 수
const OUT_SQL = process.env.OUT_SQL || './seed.sql';
const OUT_JSON = process.env.OUT_JSON || './dataset.json';

// ── JWT 사전 발급 (Java 서버의 키 유도 완전 복제) ─────────────────
// 서버: Keys.hmacShaKeyFor(Base64.getEncoder().encode(secretKey.getBytes()))
// → secretKey.getBytes() = UTF-8 바이트 → Base64.encode() → 그 바이트가 HMAC 키
let mintToken = null;
if (JWT_KEY) {
  let jwtModule;
  try { jwtModule = require('jsonwebtoken'); } catch {
    console.error('[오류] jsonwebtoken 패키지 없음. npm install jsonwebtoken 후 재실행');
    process.exit(1);
  }
  const hmacKeyBytes = Buffer.from(Buffer.from(JWT_KEY).toString('base64'));
  const keyLen = hmacKeyBytes.length;
  const alg = keyLen >= 64 ? 'HS512' : keyLen >= 48 ? 'HS384' : 'HS256';
  console.log(`[JWT] 알고리즘=${alg} | 키=${keyLen} bytes`);

  mintToken = (memberKey, role) =>
    'Bearer ' + jwtModule.sign(
      { sub: memberKey, role, iss: 'recaring' },
      hmacKeyBytes,
      { algorithm: alg, expiresIn: '24h' }
    );
} else {
  console.warn('[경고] JWT_KEY 없음 — dataset.json에 token 필드가 null로 저장됩니다.');
}

const uuid = () => crypto.randomUUID();

// phone: NOT NULL · UNIQUE · 11자리. VO 검증은 DB INSERT에서 우회되지만 형식은 맞춰 둔다.
let phoneSeq = 0;
const nextPhone = () => '010' + String(++phoneSeq).padStart(8, '0'); // 010 + 8자리 = 11자리

const subscribers = []; // { wardKey, guardianKey }  (N*G)
const wards = [];        // { wardKey, deviceToken }  (N)

const memberRows = [];   // members INSERT 값
const careRows = [];     // care_relationships INSERT 값
const tokenRows = [];    // ward_device_tokens INSERT 값

for (let w = 0; w < N; w++) {
  const wardKey = uuid();
  const deviceToken = uuid();

  wards.push({ wardKey, deviceToken });

  // WARD member
  memberRows.push(
    `('${wardKey}', '${nextPhone()}', 'ward_${w}', '1970-01-01', 'MALE', 'WARD', 'LOCAL', 'BASIC', NOW())`
  );
  // ward device token (GPS POST 인증용)
  tokenRows.push(`('${wardKey}', '${deviceToken}', NOW())`);

  for (let g = 0; g < G; g++) {
    const guardianKey = uuid();
    const role = g === 0 ? 'GUARDIAN' : 'MANAGER';
    const token = mintToken ? mintToken(guardianKey, role) : null;
    subscribers.push({ wardKey, guardianKey, token });

    // GUARDIAN member
    memberRows.push(
      `('${guardianKey}', '${nextPhone()}', 'guardian_${w}_${g}', '1990-01-01', 'FEMALE', 'GUARDIAN', 'LOCAL', 'BASIC', NOW())`
    );
    // care relationship (SSE 접근 검증용) — 첫 보호자는 GUARDIAN, 나머지는 MANAGER로 분산
    careRows.push(`('${wardKey}', '${guardianKey}', '${role}', NOW())`);
  }
}

// ── seed.sql ──────────────────────────────────────────────
const chunk = (arr, size) => {
  const out = [];
  for (let i = 0; i < arr.length; i += size) out.push(arr.slice(i, i + size));
  return out;
};
const BATCH = 1000;

const sqlParts = [];
sqlParts.push('-- 부하 테스트 시드. 테스트 후 정리: 아래 cleanup 블록 참고.');
sqlParts.push('BEGIN;');

for (const c of chunk(memberRows, BATCH)) {
  sqlParts.push(
    'INSERT INTO members (member_key, phone, name, birth, gender, role, sign_up_type, subscription_type, created_at) VALUES\n' +
      c.join(',\n') + ';'
  );
}
for (const c of chunk(careRows, BATCH)) {
  sqlParts.push(
    'INSERT INTO care_relationships (ward_member_key, caregiver_member_key, care_role, created_at) VALUES\n' +
      c.join(',\n') + ';'
  );
}
for (const c of chunk(tokenRows, BATCH)) {
  sqlParts.push(
    'INSERT INTO ward_device_tokens (ward_key, token, created_at) VALUES\n' +
      c.join(',\n') + ';'
  );
}

sqlParts.push('COMMIT;');
sqlParts.push('');
sqlParts.push('-- cleanup (테스트 종료 후 실행):');
sqlParts.push("-- DELETE FROM ward_device_tokens WHERE ward_key IN (SELECT member_key FROM members WHERE name LIKE 'ward\\_%');");
sqlParts.push("-- DELETE FROM care_relationships WHERE caregiver_member_key IN (SELECT member_key FROM members WHERE name LIKE 'guardian\\_%');");
sqlParts.push("-- DELETE FROM members WHERE name LIKE 'ward\\_%' OR name LIKE 'guardian\\_%';");

fs.writeFileSync(OUT_SQL, sqlParts.join('\n'));

// ── dataset.json ──────────────────────────────────────────
fs.writeFileSync(
  OUT_JSON,
  JSON.stringify({ guardiansPerWard: G, wards, subscribers }, null, 0)
);

console.log(`생성 완료:
  wards            = ${N}
  guardians/ward   = ${G}
  총 SSE 연결 대상 = ${subscribers.length}
  ${OUT_SQL}  (members ${memberRows.length} / care ${careRows.length} / tokens ${tokenRows.length})
  ${OUT_JSON}`);
