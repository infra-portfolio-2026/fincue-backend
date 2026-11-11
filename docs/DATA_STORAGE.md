# DB 저장 위치 (MongoDB)

MongoDB 데이터베이스: `financial_consumer` (application.properties의 `spring.data.mongodb.uri` 기준)

---

## 카드 관련

### 1. `cards` 컬렉션
- **저장 시점**: Swagger에서 **POST /api/sync/cards** (카드고릴라 동기화) 실행 시
- **내용**: 카드고릴라 API에서 가져온 신용카드 목록
- **주요 필드**:
  - `externalId`, `cardName`, `cardCompany`
  - `benefitSummary`, `benefits`, **`benefitCategories`** (혜택 분야: 주유, 푸드, 여행/숙박 등)
  - `annualFeeDomestic`, `annualFeeOverseas`, `annualFeeText`, `previousMonthCondition`
  - `detailUrl`, `crawledAt`

---

## 기타 컬렉션 (참고)

| 컬렉션 | 설명 |
|--------|------|
| `products` | FSS Open API 동기화 (정기예금/적금/연금저축) – POST /api/sync/products |
| `spendings` | 소비 내역 (userId, category, amount, spentAt) |
| `users` | 로그인 유저 (OAuth/JWT) |
| `posts`, `comments`, `likes`, `bookmarks` | 커뮤니티 게시글·댓글·좋아요·북마크 |
| `challenges`, `user_challenges` | 챌린지·유저별 도전 참여 |

---

## API로 데이터 가져오기

- **카드 목록**: 카드 동기화 후 MongoDB `cards` 컬렉션에서 직접 조회 또는 추후 카드 검색/추천 API 확장. 각 카드의 `benefitCategories` 필드에 혜택 분야(공항라운지/PP, 교통, 쇼핑 등)가 API 기준으로 저장됨.

---

## 카드 필드가 비어 있을 때

카드고릴라 API 응답 구조가 다르면 `cardCompany`, `benefitSummary`, `benefits`, `annualFeeText` 등이 비어 있을 수 있습니다.

1. 브라우저 개발자도구 → Network → `cards?p=1&perPage=...` 요청 선택 → **Response** 또는 **Preview** 탭에서 JSON 구조 확인.
2. 실제 필드명(예: `brand_name`, `promo_text`, `fee_info`)을 확인한 뒤 `CardGorillaSyncService.mapNodeToCard()` 의 `text()`, `textFromPath()`, `collectBenefits()` 등에 해당 키를 추가.
3. 로그 레벨을 `DEBUG`로 올리면 동기화 시 "카드고릴라 API 첫 항목 필드: [...]" 로그로 첫 카드의 필드명을 확인할 수 있습니다.
