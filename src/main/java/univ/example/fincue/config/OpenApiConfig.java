package univ.example.fincue.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI customOpenAPI() {
        Operation syncProductsOp = new Operation()
                .operationId("syncProducts")
                .summary("FSS 금융상품 DB 저장")
                .description("FSS Open API를 호출해 정기예금·적금·연금저축 각 최대 300건씩 가져와 MongoDB products 컬렉션에 저장합니다. 인증 불필요. 실행 후 서버 로그에서 건수 확인.")
                .addTagsItem("FSS Open API 동기화")
                .security(Collections.emptyList())
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("동기화 요청 완료. 로그에서 결과 확인.")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()))));

        Operation syncCardsOp = new Operation()
                .operationId("syncCards")
                .summary("카드 목록 DB 저장")
                .description("카드고릴라 API를 호출해 신용카드 목록을 가져와 MongoDB cards 컬렉션에 저장합니다. 인증 불필요. 실행 후 서버 로그에서 건수 확인.")
                .addTagsItem("카드고릴라 동기화")
                .security(Collections.emptyList())
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("카드 동기화 요청 완료. 로그에서 결과 확인.")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType()))));

        return new OpenAPI()
                .info(new Info()
                        .title("AI 금융 관리 API")
                        .description("소비 패턴 분석·절약 챌린지 기반 맞춤형 자산 관리 서비스 백엔드 API. " +
                                "• FSS Open API 동기화: Swagger에서 'FSS Open API 동기화' → POST /api/sync/products 실행 시 DB 저장. " +
                                "• 인증: OAuth2(구글·카카오·네이버) 로그인 후 JWT, 또는 POST /auth/kakao/token 으로 카카오 access_token 전달 시 JWT 발급. " +
                                "• 게시글 카테고리: 절약팁, 질문, 자유. " +
                                "• 대표 TIP/실시간 HOT: 일주일 이내 좋아요 상위 3개. " +
                                "• 오늘의 인기글: 당일 작성 + 좋아요 5개 이상. " +
                                "• 나만의 챌린지: 요약, 진행중/완료 목록, 새 도전 만들기, 수정, 인기 도전, 함께하기. " +
                                "로그인 필요 API는 'Authorize' 버튼에서 JWT 입력.")
                        .version("1.0"))
                .path("/api/sync/products", new PathItem().post(syncProductsOp))
                .path("/api/sync/cards", new PathItem().post(syncCardsOp))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("OAuth2 로그인 또는 POST /auth/kakao/token 으로 발급받은 JWT. Bearer 접두사 없이 토큰만 입력")));
    }

    /** 스캔된 문서 병합 후 GET /auth/me 를 추가해 Swagger에 확실히 노출 */
    @Bean
    public OpenApiCustomizer authMePathCustomizer() {
        Operation getCurrentUserNameOp = new Operation()
                .operationId("getCurrentUserName")
                .summary("현재 유저 이름")
                .description("Authorization 헤더의 JWT로 인증된 유저의 이름을 반환합니다. 파라미터 없이 Bearer 토큰만 보내면 됩니다.")
                .addTagsItem("인증")
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse()
                                .description("유저 이름 반환. body: {\"name\": \"이름\"}")
                                .content(new Content()
                                        .addMediaType("application/json", new MediaType())))
                        .addApiResponse("401", new ApiResponse()
                                .description("JWT 없음 또는 유효하지 않음")));

        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                paths = new Paths();
                openApi.setPaths(paths);
            }
            paths.addPathItem("/auth/me", new PathItem().get(getCurrentUserNameOp));
        };
    }

    /** 스캔 누락 대비: POST /api/incomes 를 Swagger에 명시 추가 */
    @Bean
    public OpenApiCustomizer incomePathCustomizer() {
        Operation addIncomeOp = new Operation()
                .operationId("addIncome")
                .summary("수입 내역 추가")
                .description("로그인 필요. receivedDate(yyyy-MM-dd), category, amount를 입력하여 수입 내역을 추가합니다.")
                .addTagsItem("수입")
                .responses(new ApiResponses()
                        .addApiResponse("201", new ApiResponse().description("수입 내역 추가 성공"))
                        .addApiResponse("400", new ApiResponse().description("날짜/금액 누락 또는 날짜 형식 오류"))
                        .addApiResponse("401", new ApiResponse().description("로그인 필요")));

        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                paths = new Paths();
                openApi.setPaths(paths);
            }
            PathItem pathItem = paths.get("/api/incomes");
            if (pathItem == null) {
                pathItem = new PathItem();
            }
            pathItem.post(addIncomeOp);
            paths.addPathItem("/api/incomes", pathItem);
        };
    }

    /** 스캔 누락 대비: GET /api/spendings/daily-cashflow 를 Swagger에 명시 추가 */
    @Bean
    public OpenApiCustomizer spendingDailyCashflowCustomizer() {
        Parameter yearParam = new Parameter()
                .in("query")
                .name("year")
                .required(false)
                .description("조회할 연도. 생략 시 이번 해")
                .schema(new IntegerSchema().example(2026));

        Parameter monthParam = new Parameter()
                .in("query")
                .name("month")
                .required(false)
                .description("조회할 월(1~12). 생략 시 이번 달")
                .schema(new IntegerSchema().example(5));

        Operation dailyCashflowOp = new Operation()
                .operationId("getDailyCashflow")
                .summary("일자별 수입/지출")
                .description("로그인한 유저의 지정 연·월에 대해 날짜별 수입/지출/순금액을 반환합니다. year/month 생략 시 이번 달.")
                .addTagsItem("소비")
                .addParametersItem(yearParam)
                .addParametersItem(monthParam)
                .responses(new ApiResponses()
                        .addApiResponse("200", new ApiResponse().description("조회 성공"))
                        .addApiResponse("401", new ApiResponse().description("로그인 필요")));

        return openApi -> {
            Paths paths = openApi.getPaths();
            if (paths == null) {
                paths = new Paths();
                openApi.setPaths(paths);
            }
            PathItem pathItem = paths.get("/api/spendings/daily-cashflow");
            if (pathItem == null) {
                pathItem = new PathItem();
            }
            pathItem.get(dailyCashflowOp);
            paths.addPathItem("/api/spendings/daily-cashflow", pathItem);
        };
    }
}

