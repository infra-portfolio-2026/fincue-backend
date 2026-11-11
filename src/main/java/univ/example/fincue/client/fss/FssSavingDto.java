package univ.example.fincue.client.fss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 적금 API 응답: savingProductsSearch.json
 * 구조는 정기예금과 동일 (baseList + optionList, save_trm, intr_rate, intr_rate2)
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FssSavingDto {

    @JsonProperty("result")
    private SavingResult result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavingResult {
        @JsonProperty("err_cd")
        private String errCd;
        @JsonProperty("err_msg")
        private String errMsg;
        @JsonProperty("total_count")
        private String totalCount;
        @JsonProperty("baseList")
        private List<SavingBase> baseList;
        @JsonProperty("optionList")
        private List<SavingOption> optionList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class SavingBase {
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
    public static class SavingOption {
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("save_trm")
        private String saveTrm;
        @JsonProperty("intr_rate")
        private Double intrRate;
        @JsonProperty("intr_rate2")
        private Double intrRate2;
    }
}
