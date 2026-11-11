package univ.example.fincue.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import univ.example.fincue.client.fss.FssAnnuitySavingDto;
import univ.example.fincue.client.fss.FssDepositDto;
import univ.example.fincue.client.fss.FssSavingDto;
import univ.example.fincue.document.ProductDocument;
import univ.example.fincue.repository.mongo.ProductMongoRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * FSS 금융상품 API 호출 후 MongoDB products 컬렉션에 각 300건씩 동기화
 * - 정기예금: depositProductsSearch (은행 020000)
 * - 적금: savingProductsSearch (은행 020000)
 * - 연금저축: annuitySavingProductsSearch (금융투자 060000)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FssProductSyncService {

    private static final String FSS_BASE = "https://finlife.fss.or.kr/finlifeapi";
    private static final int TARGET_COUNT = 300;
    private static final int PAGE_SIZE = 100;
    private static final String DEPOSIT = "DEPOSIT";
    private static final String SAVING = "SAVING";
    private static final String PENSION = "PENSION";

    private final RestTemplate restTemplate;
    private final ProductMongoRepository productMongoRepository;

    @Value("${fss.api.key:}")
    private String fssApiKey;

    public void syncAll() {
        if (fssApiKey == null || fssApiKey.isBlank()) {
            log.warn("fss.api.key가 비어 있어 동기화를 건너뜁니다.");
            return;
        }
        syncDeposit();
        syncSaving();
        syncPension();
    }

    public void syncDeposit() {
        String url = FSS_BASE + "/depositProductsSearch.json?auth={auth}&topFinGrpNo=020000&pageNo={pageNo}";
        List<ProductDocument> list = new ArrayList<>();
        for (int pageNo = 1; pageNo <= (TARGET_COUNT / PAGE_SIZE) + 1; pageNo++) {
            ResponseEntity<FssDepositDto> res = restTemplate.getForEntity(url, FssDepositDto.class, fssApiKey, pageNo);
            FssDepositDto body = res.getBody();
            if (body == null || body.getResult() == null) continue;
            FssDepositDto.DepositResult result = body.getResult();
            if (!"000".equals(result.getErrCd())) {
                log.warn("정기예금 API 오류: {} {}", result.getErrCd(), result.getErrMsg());
                break;
            }
            Map<String, FssDepositDto.DepositBase> baseMap = (result.getBaseList() == null ? Collections.<FssDepositDto.DepositBase>emptyList() : result.getBaseList())
                    .stream().collect(Collectors.toMap(b -> b.getFinCoNo() + "_" + b.getFinPrdtCd(), b -> b, (a, b) -> a));
            List<FssDepositDto.DepositOption> options = result.getOptionList() == null ? Collections.emptyList() : result.getOptionList();
            for (FssDepositDto.DepositOption opt : options) {
                if (list.size() >= TARGET_COUNT) break;
                String key = opt.getFinCoNo() + "_" + opt.getFinPrdtCd();
                FssDepositDto.DepositBase base = baseMap.get(key);
                if (base == null) continue;
                ProductDocument doc = ProductDocument.builder()
                        .productType(DEPOSIT)
                        .finCoNo(base.getFinCoNo())
                        .finPrdtCd(base.getFinPrdtCd())
                        .bankName(base.getKorCoNm())
                        .productName(base.getFinPrdtNm())
                        .interestRate(opt.getIntrRate() != null ? BigDecimal.valueOf(opt.getIntrRate()) : null)
                        .intrRate2(opt.getIntrRate2() != null ? BigDecimal.valueOf(opt.getIntrRate2()) : null)
                        .period(parseInt(opt.getSaveTrm()))
                        .maxAmount(base.getMaxLimit() != null ? BigDecimal.valueOf(base.getMaxLimit()) : null)
                        .crawledAt(Instant.now())
                        .build();
                list.add(doc);
            }
            if (options.size() < PAGE_SIZE) break;
        }
        saveType(DEPOSIT, list);
    }

    public void syncSaving() {
        String url = FSS_BASE + "/savingProductsSearch.json?auth={auth}&topFinGrpNo=020000&pageNo={pageNo}";
        List<ProductDocument> list = new ArrayList<>();
        for (int pageNo = 1; pageNo <= (TARGET_COUNT / PAGE_SIZE) + 1; pageNo++) {
            ResponseEntity<FssSavingDto> res = restTemplate.getForEntity(url, FssSavingDto.class, fssApiKey, pageNo);
            FssSavingDto body = res.getBody();
            if (body == null || body.getResult() == null) continue;
            FssSavingDto.SavingResult result = body.getResult();
            if (!"000".equals(result.getErrCd())) {
                log.warn("적금 API 오류: {} {}", result.getErrCd(), result.getErrMsg());
                break;
            }
            Map<String, FssSavingDto.SavingBase> baseMap = (result.getBaseList() == null ? Collections.<FssSavingDto.SavingBase>emptyList() : result.getBaseList())
                    .stream().collect(Collectors.toMap(b -> b.getFinCoNo() + "_" + b.getFinPrdtCd(), b -> b, (a, b) -> a));
            List<FssSavingDto.SavingOption> options = result.getOptionList() == null ? Collections.emptyList() : result.getOptionList();
            for (FssSavingDto.SavingOption opt : options) {
                if (list.size() >= TARGET_COUNT) break;
                String key = opt.getFinCoNo() + "_" + opt.getFinPrdtCd();
                FssSavingDto.SavingBase base = baseMap.get(key);
                if (base == null) continue;
                ProductDocument doc = ProductDocument.builder()
                        .productType(SAVING)
                        .finCoNo(base.getFinCoNo())
                        .finPrdtCd(base.getFinPrdtCd())
                        .bankName(base.getKorCoNm())
                        .productName(base.getFinPrdtNm())
                        .interestRate(opt.getIntrRate() != null ? BigDecimal.valueOf(opt.getIntrRate()) : null)
                        .intrRate2(opt.getIntrRate2() != null ? BigDecimal.valueOf(opt.getIntrRate2()) : null)
                        .period(parseInt(opt.getSaveTrm()))
                        .maxAmount(base.getMaxLimit() != null ? BigDecimal.valueOf(base.getMaxLimit()) : null)
                        .crawledAt(Instant.now())
                        .build();
                list.add(doc);
            }
            if (options.size() < PAGE_SIZE) break;
        }
        saveType(SAVING, list);
    }

    public void syncPension() {
        String url = FSS_BASE + "/annuitySavingProductsSearch.json?auth={auth}&topFinGrpNo=060000&pageNo={pageNo}";
        List<ProductDocument> list = new ArrayList<>();
        for (int pageNo = 1; pageNo <= (TARGET_COUNT / PAGE_SIZE) + 1; pageNo++) {
            ResponseEntity<FssAnnuitySavingDto> res = restTemplate.getForEntity(url, FssAnnuitySavingDto.class, fssApiKey, pageNo);
            FssAnnuitySavingDto body = res.getBody();
            if (body == null || body.getResult() == null) continue;
            FssAnnuitySavingDto.AnnuityResult result = body.getResult();
            if (!"000".equals(result.getErrCd())) {
                log.warn("연금저축 API 오류: {} {}", result.getErrCd(), result.getErrMsg());
                break;
            }
            List<FssAnnuitySavingDto.AnnuityBase> bases = result.getBaseList() == null ? Collections.emptyList() : result.getBaseList();
            for (FssAnnuitySavingDto.AnnuityBase base : bases) {
                if (list.size() >= TARGET_COUNT) break;
                ProductDocument doc = ProductDocument.builder()
                        .productType(PENSION)
                        .finCoNo(base.getFinCoNo())
                        .finPrdtCd(base.getFinPrdtCd())
                        .bankName(base.getKorCoNm())
                        .productName(base.getFinPrdtNm())
                        .interestRate(base.getAvgPrftRate() != null ? BigDecimal.valueOf(base.getAvgPrftRate()) : null)
                        .period(null)
                        .crawledAt(Instant.now())
                        .build();
                list.add(doc);
            }
            if (bases.size() < PAGE_SIZE) break;
        }
        saveType(PENSION, list);
    }

    private void saveType(String productType, List<ProductDocument> list) {
        productMongoRepository.deleteByProductType(productType);
        if (!list.isEmpty()) {
            productMongoRepository.saveAll(list);
            log.info("FSS {} 동기화 완료: {}건", productType, list.size());
        }
    }

    private static Integer parseInt(String saveTrm) {
        if (saveTrm == null || saveTrm.isBlank()) return null;
        try {
            return Integer.parseInt(saveTrm.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
