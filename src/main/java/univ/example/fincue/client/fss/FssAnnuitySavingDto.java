package univ.example.fincue.client.fss;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 연금저축 API 응답: annuitySavingProductsSearch.json
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FssAnnuitySavingDto {

    @JsonProperty("result")
    private AnnuityResult result;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnnuityResult {
        @JsonProperty("err_cd")
        private String errCd;
        @JsonProperty("err_msg")
        private String errMsg;
        @JsonProperty("total_count")
        private String totalCount;
        @JsonProperty("baseList")
        private List<AnnuityBase> baseList;
        @JsonProperty("optionList")
        private List<AnnuityOption> optionList;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnnuityBase {
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
        @JsonProperty("pnsn_kind_nm")
        private String pnsnKindNm;
        @JsonProperty("prdt_type_nm")
        private String prdtTypeNm;
        @JsonProperty("avg_prft_rate")
        private Double avgPrftRate;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AnnuityOption {
        @JsonProperty("fin_co_no")
        private String finCoNo;
        @JsonProperty("fin_prdt_cd")
        private String finPrdtCd;
        @JsonProperty("paym_prd")
        private String paymPrd;   // 납입기간(년)
        @JsonProperty("pnsn_recp_amt")
        private Long pnsnRecpAmt;
    }
}
