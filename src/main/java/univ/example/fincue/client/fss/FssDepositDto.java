package univ.example.fincue.client.fss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 정기예금 API 응답: depositProductsSearch.json
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FssDepositDto {

    @JsonProperty("result")
    private DepositResult result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepositResult {
        @JsonProperty("err_cd")
        private String errCd;
        @JsonProperty("err_msg")
        private String errMsg;
        @JsonProperty("total_count")
        private String totalCount;
        @JsonProperty("baseList")
        private List<DepositBase> baseList;
        @JsonProperty("optionList")
        private List<DepositOption> optionList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepositBase {
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("kor_co_nm")
        private String korCoNm;
        @JsonProperty("fin_prdt_nm")
        private String finPrdtNm;
        @JsonProperty("join_way")
        private String joinWay;
        @JsonProperty("max_limit")
        private Long maxLimit;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DepositOption {
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("save_trm")
        private String saveTrm;   // 개월
        @JsonProperty("intr_rate")
        private Double intrRate;
        @JsonProperty("intr_rate2")
        private Double intrRate2;
    }
}
