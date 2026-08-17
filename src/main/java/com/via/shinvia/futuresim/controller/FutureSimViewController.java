package com.via.shinvia.futuresim.controller;

import com.via.shinvia.futuresim.entity.FuturesimHouseholdMember;
import com.via.shinvia.futuresim.service.CurrentStatusService;
import com.via.shinvia.futuresim.service.HouseholdMemberService;
import com.via.shinvia.security.CurrentUser;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

// 미래 금융 시뮬레이터 1단계("지금 내 상태") 화면
@Controller
@RequestMapping("/futuresim")
@RequiredArgsConstructor
public class FutureSimViewController {

    public static final String COMPARISON_BASIS_SESSION_KEY = "futuresimComparisonBasis";
    public static final String HOUSEHOLD_SIZE_SESSION_KEY = "futuresimHouseholdSize";
    public static final String GOAL_AMOUNT_SESSION_KEY = "futuresimGoalAmount";

    private final CurrentUser currentUser;
    private final CurrentStatusService currentStatusService;
    private final HouseholdMemberService householdMemberService;

    @GetMapping
    public String currentStatus(Authentication authentication, HttpSession session, Model model) {
        Long userId = currentUser.getUserId(authentication);
        List<FuturesimHouseholdMember> householdMembers = householdMemberService.getDraftMembers(session);
        // 본인(1인)은 이미 있는 정보이므로, 가구원을 추가/삭제할 때마다 가구원수가 자동으로
        // 늘고 줄게 한다 — 드롭다운으로 따로 고르게 하지 않는다.
        String householdSize = resolveHouseholdSizeLabel(1 + householdMembers.size());
        session.setAttribute(HOUSEHOLD_SIZE_SESSION_KEY, householdSize);

        CurrentStatusService.CurrentStatusView status =
                currentStatusService.getCurrentStatus(userId, householdSize);

        model.addAttribute("status", status);
        model.addAttribute("householdMembers", householdMembers);
        addStepperAttributes(session, model);
        // layout/basic.html이 serviceName == '금융 라이프 플랜'일 때 자산관리 사이드바(assetServiceMenus)를 그려주므로,
        // LayoutModelAdvice의 기본값("금융 진단센터", /futuresim 경로는 매칭 안 됨)을 여기서 덮어써서
        // 금융 라이프 플랜 프레임을 그대로 재사용한다. LayoutModelAdvice 자체는 건드리지 않음.
        model.addAttribute("serviceName", "금융 라이프 플랜");

        return "futuresim/current-status";
    }

    // 3단계 스테퍼 어디서든 쓰는 잠금 조건 — 2단계는 비교 기준을 골라야, 3단계는 목표 금액을
    // 저장해야 의미 있는 화면이 나오므로, 아직 안 거쳤으면 해당 탭을 잠가서 건너뛰지 못하게 한다.
    private void addStepperAttributes(HttpSession session, Model model) {
        model.addAttribute("comparisonBasisSelected", session.getAttribute(COMPARISON_BASIS_SESSION_KEY) != null);
        model.addAttribute("goalAmountSaved", session.getAttribute(GOAL_AMOUNT_SESSION_KEY) != null);
    }

    // "이 기준으로 계속" 클릭 시 선택 기준(AGE/HOUSEHOLD)을 세션에 저장하고 2단계(목표 설정)로 넘어간다.
    @PostMapping("/comparison-basis")
    public String selectComparisonBasis(@RequestParam String basis, HttpSession session) {
        session.setAttribute(COMPARISON_BASIS_SESSION_KEY, basis);
        return "redirect:/futuresim/goal";
    }

    // 1(본인) + 가구원 수를 KOSIS 벤치마크 라벨로 변환한다.
    private static String resolveHouseholdSizeLabel(int totalMembers) {
        return switch (Math.min(totalMembers, 5)) {
            case 1 -> "1인";
            case 2 -> "2인";
            case 3 -> "3인";
            case 4 -> "4인";
            default -> "5인이상";
        };
    }

    // 2단계("목표 설정") 화면. 프리셋 카드 목록 등은 아직 없어서, 이번 작업 범위(가구 프로필 요약 카드)에
    // 필요한 최소한의 뼈대만 렌더링한다.
    @GetMapping("/goal")
    public String goalPresets(HttpSession session, Model model) {
        if (session.getAttribute(COMPARISON_BASIS_SESSION_KEY) == null) {
            return "redirect:/futuresim";
        }
        addStepperAttributes(session, model);
        model.addAttribute("serviceName", "금융 라이프 플랜");
        return "futuresim/goal-preset";
    }

    // 3단계("성장 곡선") 화면. 데이터는 전부 /api/future-simulation/projection에서 JS로 받아오므로
    // 여기서는 뼈대만 렌더링한다 — 2단계 goalPresets()와 같은 패턴.
    @GetMapping("/growth")
    public String growthProjection(HttpSession session, Model model) {
        addStepperAttributes(session, model);
        model.addAttribute("goalAmount", session.getAttribute(GOAL_AMOUNT_SESSION_KEY));
        model.addAttribute("serviceName", "금융 라이프 플랜");
        return "futuresim/growth";
    }

    // 4단계("레버 랭킹") 화면. 3단계 growthProjection()과 같은 패턴 — 데이터는 전부 JS가
    // /api/future-simulation/lever-* 에서 받아온다. goalAmount는 2단계에서 저장한 세션값을 그대로 재사용.
    @GetMapping("/levers")
    public String leverRanking(HttpSession session, Model model) {
        addStepperAttributes(session, model);
        model.addAttribute("goalAmount", session.getAttribute(GOAL_AMOUNT_SESSION_KEY));
        model.addAttribute("serviceName", "금융 라이프 플랜");
        return "futuresim/levers";
    }

    // 5단계("레버 조합해보기") 화면. 4단계 leverRanking()과 같은 패턴 — 체크박스 상태/시뮬레이션 결과는
    // 전부 JS가 /api/future-simulation/combo-simulation, /plan에서 처리한다.
    @GetMapping("/combo")
    public String leverCombo(HttpSession session, Model model) {
        addStepperAttributes(session, model);
        model.addAttribute("goalAmount", session.getAttribute(GOAL_AMOUNT_SESSION_KEY));
        model.addAttribute("serviceName", "금융 라이프 플랜");
        return "futuresim/combo";
    }

    // 가구원 추가 — DB에 바로 안 넣고 세션 초안에만 담는다. "가구원 정보 저장"을 눌러야 커밋됨.
    @PostMapping("/household-members")
    public String addHouseholdMember(
            @RequestParam String memberName,
            @RequestParam String relationship,
            @RequestParam int age,
            @RequestParam(required = false) BigDecimal annualIncome,
            HttpSession session
    ) {
        householdMemberService.addDraftMember(session, memberName, relationship, age, annualIncome);
        return "redirect:/futuresim";
    }

    @PostMapping("/household-members/remove")
    public String removeHouseholdMember(@RequestParam int index, HttpSession session) {
        householdMemberService.removeDraftMember(session, index);
        return "redirect:/futuresim";
    }

    // "다음" 클릭 시 목표 금액을 세션에 저장하고 3단계(성장 곡선)로 넘어간다.
    @PostMapping("/goal-amount")
    public String saveGoalAmount(@RequestParam BigDecimal goalAmount, HttpSession session) {
        session.setAttribute(GOAL_AMOUNT_SESSION_KEY, goalAmount);
        return "redirect:/futuresim/growth";
    }
}
