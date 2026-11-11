package univ.example.fincue.controller.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import univ.example.fincue.document.ChallengeDocument;
import univ.example.fincue.document.UserChallengeDocument;
import univ.example.fincue.dto.*;
import univ.example.fincue.service.ChallengeService;
import univ.example.fincue.service.CurrentUserService;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/challenges")
@RequiredArgsConstructor
@Tag(name = "나만의 챌린지", description = "챌린지 요약, 진행중/완료 목록, 새 도전 만들기, 수정, 인기 도전, 함께하기")
public class ChallengeApiController {

    private final ChallengeService challengeService;
    private final CurrentUserService currentUserService;

    @GetMapping("/summary")
    @Operation(summary = "챌린지 요약", description = "진행 중인 도전 개수, 성공한 도전 개수, 총 절약 금액")
    public ResponseEntity<?> getSummary() {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(challengeService.getSummary(userId.get()));
    }

    @GetMapping("/my/ongoing")
    @Operation(summary = "진행 중인 도전 목록", description = "기간이 아직 끝나지 않은 내 도전 목록")
    public ResponseEntity<?> getMyOngoing() {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(challengeService.getMyOngoingChallenges(userId.get()));
    }

    @GetMapping("/my/completed")
    @Operation(summary = "완료된 도전 목록", description = "기간이 종료된 내 도전 목록 (성공/실패 포함)")
    public ResponseEntity<?> getMyCompleted() {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        return ResponseEntity.ok(challengeService.getMyCompletedChallenges(userId.get()));
    }

    @PostMapping
    @Operation(summary = "새 도전 만들기", description = "제목, 카테고리, 목표 금액, 시작일, 종료일로 나만의 도전 생성")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "도전 생성 성공"),
            @ApiResponse(responseCode = "400", description = "필수 값 누락 또는 날짜 형식 오류"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> createChallenge(@RequestBody CreateChallengeRequest request) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body("제목을 입력해주세요.");
        }
        if (request.getGoalAmount() == null) {
            return ResponseEntity.badRequest().body("목표 금액을 입력해주세요.");
        }
        if (request.getStartDate() == null || request.getStartDate().isBlank() ||
                request.getEndDate() == null || request.getEndDate().isBlank()) {
            return ResponseEntity.badRequest().body("시작일과 종료일을 입력해주세요.");
        }
        try {
            ChallengeDocument created = challengeService.createChallenge(userId.get(), request);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("날짜 형식이 올바르지 않습니다. (yyyy-MM-dd)");
        }
    }

    @PutMapping("/{challengeId}")
    @Operation(summary = "도전 수정", description = "진행 중인 도전만 수정 가능. 본인이 생성한 도전만 수정 가능.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공"),
            @ApiResponse(responseCode = "400", description = "권한 없음 또는 완료된 도전"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> updateChallenge(
            @PathVariable String challengeId,
            @RequestBody UpdateChallengeRequest request) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        try {
            ChallengeDocument updated = challengeService.updateChallenge(userId.get(), challengeId, request);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{challengeId}")
    @Operation(summary = "도전 삭제", description = "본인이 생성한 도전만 삭제 가능. 관련 참여 기록도 함께 삭제.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "400", description = "권한 없음 또는 도전 없음"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> deleteChallenge(@PathVariable String challengeId) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        try {
            challengeService.deleteChallenge(userId.get(), challengeId);
            return ResponseEntity.ok("챌린지가 삭제되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/popular")
    @Operation(summary = "사람들이 많이 한 도전", description = "참여 인원 순 인기 도전 목록 (로그인 불필요)")
    public ResponseEntity<List<PopularChallengeDto>> getPopular(
            @Parameter(description = "최대 개수") @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(challengeService.getPopularChallenges(limit));
    }

    @PostMapping("/{challengeId}/join")
    @Operation(summary = "도전 함께하기", description = "인기 도전에 참여. 이미 참여 중이면 에러.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "참여 성공"),
            @ApiResponse(responseCode = "400", description = "이미 참여 중 또는 도전 없음"),
            @ApiResponse(responseCode = "401", description = "로그인 필요")
    })
    public ResponseEntity<?> joinChallenge(@PathVariable String challengeId) {
        Optional<String> userId = currentUserService.getCurrentMongoUserId();
        if (userId.isEmpty()) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        try {
            UserChallengeDocument joined = challengeService.joinChallenge(userId.get(), challengeId);
            return ResponseEntity.status(HttpStatus.CREATED).body(joined);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
