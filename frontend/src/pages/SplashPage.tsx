import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { motion, useReducedMotion, type Variants } from "framer-motion";
import { useAuthStore } from "@/store/authStore";

// 애니메이션 타이밍 상수
const ANIMATION_TIMING = {
  /** 태그라인이 보여진 뒤 사용자가 읽을 시간 (ms) */
  READ_HOLD_MS: 900,
  /** reduced motion 모드에서 스플래시 노출 시간 (ms) */
  REDUCED_MOTION_DELAY_MS: 900,
} as const;

// 배경 그라디언트 스타일 상수
const BACKGROUND_STYLES = {
  /** 메인 배경 (밝은 공항 톤) */
  main: `
    radial-gradient(920px 600px at 18% 10%, rgba(0,100,255,0.14), rgba(0,100,255,0) 62%),
    radial-gradient(820px 560px at 90% 88%, rgba(90,220,255,0.14), rgba(90,220,255,0) 62%),
    linear-gradient(180deg, #FFFFFF 0%, #F8FBFF 55%, #EEF6FF 100%)
  `.trim(),
  /** 글로우 효과 */
  glow: "radial-gradient(closest-side, rgba(0,100,255,0.16), rgba(0,100,255,0))",
  /** 로고 주변 글로우 */
  logoGlow: "radial-gradient(closest-side, rgba(0,100,255,0.14), rgba(0,100,255,0))",
  /** 워드마크 광택 효과 */
  sheen: "linear-gradient(120deg, rgba(255,255,255,0) 0%, rgba(255,255,255,0.55) 14%, rgba(255,255,255,0) 28%)",
  /** PORTER 텍스트 그라디언트 */
  porterText: "linear-gradient(90deg, #0064FF 0%, #2B7BFF 42%, #5AD7FF 100%)",
  /** 노이즈 텍스처 SVG */
  noise: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='160' height='160'%3E%3Cfilter id='n'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='160' height='160' filter='url(%23n)' opacity='.28'/%3E%3C/svg%3E")`,
} as const;

// 애니메이션 variants 타입 정의
type AnimationVariants = {
  page: Variants;
  block: Variants;
  logo: Variants;
  word: Variants;
};

// 애니메이션 variants 생성 함수
const createAnimationVariants = (reduce: boolean | null): AnimationVariants => {
  if (reduce) {
    return {
      page: { initial: { opacity: 1 }, animate: { opacity: 1 } },
      block: { initial: { opacity: 1, y: 0 }, animate: { opacity: 1, y: 0 } },
      logo: { initial: { opacity: 1, scale: 1 }, animate: { opacity: 1, scale: 1 } },
      word: { initial: { opacity: 1, y: 0 }, animate: { opacity: 1, y: 0 } },
    };
  }
  return {
    page: {
      initial: { opacity: 0 },
      animate: { opacity: 1, transition: { duration: 0.55, ease: [0.22, 1, 0.36, 1] } },
    },
    block: {
      initial: { opacity: 0, y: 12 },
      animate: { opacity: 1, y: 0, transition: { duration: 0.8, ease: [0.22, 1, 0.36, 1] } },
    },
    logo: {
      initial: { opacity: 0, scale: 0.98 },
      animate: {
        opacity: 1,
        scale: 1,
        transition: { duration: 0.9, delay: 0.05, type: "spring", stiffness: 110, damping: 18 },
      },
    },
    word: {
      initial: { opacity: 0, y: 10 },
      animate: {
        opacity: 1,
        y: 0,
        transition: { duration: 0.9, delay: 0.14, ease: [0.22, 1, 0.36, 1] },
      },
    },
  };
};

type TaglineRevealProps = {
  text: string;
  onDone?: () => void;
};

/**
 * 태그라인 텍스트를 부드럽게 나타나게 하는 컴포넌트
 * - 블러 효과와 함께 페이드인
 * - 마스크 슬라이드 애니메이션으로 텍스트 공개
 */
const TaglineReveal = ({ text, onDone }: TaglineRevealProps) => {
  const reduce = useReducedMotion();

  return (
    <motion.p
      className="font-['Pretendard'] text-[clamp(1.0rem,1.6vw,1.2rem)] font-medium leading-relaxed text-slate-700 tracking-[-0.01em]"
      initial={reduce ? { opacity: 1 } : { opacity: 0, y: 8, filter: "blur(6px)" }}
      animate={reduce ? { opacity: 1 } : { opacity: 1, y: 0, filter: "blur(0px)" }}
      transition={reduce ? {} : { duration: 0.75, delay: 0.18, ease: [0.22, 1, 0.36, 1] }}
      onAnimationComplete={() => onDone?.()}
    >
      {/* 텍스트 공개 마스크 */}
      <span className="relative inline-block overflow-hidden align-bottom">
        <motion.span
          aria-hidden
          className="absolute inset-0"
          style={{ background: "#FFFFFF" }}
          initial={reduce ? { x: "100%" } : { x: "0%" }}
          animate={{ x: "100%" }}
          transition={reduce ? {} : { duration: 0.7, delay: 0.18, ease: [0.22, 1, 0.36, 1] }}
        />
        <span>{text}</span>
      </span>
    </motion.p>
  );
};

/**
 * 스플래시 페이지
 * - 앱 진입 시 브랜드 로고와 태그라인을 애니메이션으로 표시
 * - 애니메이션 완료 후 자동으로 로그인 페이지로 이동
 */
const SplashPage = () => {
  const navigate = useNavigate();
  const reduce = useReducedMotion();
  const { isAuthenticated } = useAuthStore();

  const tagline = "가장 낮은 눈높이에서, 가장 높은 서비스를";
  const [taglineDone, setTaglineDone] = useState(false);

  // 애니메이션 variants (reduce motion 여부에 따라 다르게 적용)
  const variants = createAnimationVariants(reduce);

  // reduced motion 모드: 짧게 노출 후 이동 (인증 상태에 따라 분기)
  useEffect(() => {
    if (!reduce) return;
    const timer = setTimeout(
      () => navigate(isAuthenticated ? "/home" : "/login"),
      ANIMATION_TIMING.REDUCED_MOTION_DELAY_MS
    );
    return () => clearTimeout(timer);
  }, [reduce, navigate, isAuthenticated]);

  // 일반 모드: 태그라인 애니메이션 완료 후 읽을 시간을 보장한 뒤 이동 (인증 상태에 따라 분기)
  useEffect(() => {
    if (reduce || !taglineDone) return;
    const timer = setTimeout(
      () => navigate(isAuthenticated ? "/home" : "/login"),
      ANIMATION_TIMING.READ_HOLD_MS
    );
    return () => clearTimeout(timer);
  }, [taglineDone, reduce, navigate, isAuthenticated]);

  return (
    <motion.div
      className="relative min-h-dvh w-full overflow-hidden"
      variants={variants.page}
      initial="initial"
      animate="animate"
    >
      {/* 메인 배경 - 밝은 공항 톤 그라디언트 */}
      <div
        className="absolute inset-0"
        style={{ background: BACKGROUND_STYLES.main }}
      />

      {/* 미세한 노이즈 텍스처 오버레이 */}
      <div
        aria-hidden
        className="pointer-events-none absolute inset-0 opacity-[0.06] mix-blend-multiply"
        style={{ backgroundImage: BACKGROUND_STYLES.noise }}
      />

      {/* 우상단 부드러운 글로우 효과 */}
      <motion.div
        aria-hidden
        className="pointer-events-none absolute -top-28 -right-36 h-[520px] w-[520px] rounded-full blur-3xl"
        style={{ background: BACKGROUND_STYLES.glow }}
        animate={reduce ? {} : { scale: [1, 1.03, 1], opacity: [0.88, 1, 0.88] }}
        transition={reduce ? {} : { duration: 8, repeat: Infinity, ease: "easeInOut" }}
      />

      {/* 메인 콘텐츠 영역 */}
      <div className="relative z-10 mx-auto flex min-h-dvh max-w-screen-2xl items-center justify-center px-6 sm:px-10 lg:px-16">
        <motion.div variants={variants.block} className="w-full">
          {/* 로고 + 워드마크 그리드 레이아웃 */}
          <div className="mx-auto grid w-full max-w-[1100px] items-center gap-10 md:grid-cols-[auto,1fr] md:gap-16">
            {/* 로고 이미지 */}
            <motion.div variants={variants.logo} className="relative mx-auto md:mx-0">
              <div
                aria-hidden
                className="pointer-events-none absolute -inset-10 rounded-[40px] blur-3xl"
                style={{ background: BACKGROUND_STYLES.logoGlow }}
              />
              <img
                src="/images/logo.png"
                alt="CARRY PORTER Logo"
                className="h-[clamp(72px,10vw,132px)] w-[clamp(72px,10vw,132px)] object-contain drop-shadow-[0_14px_24px_rgba(0,60,140,0.14)]"
              />
            </motion.div>

            {/* 워드마크 (브랜드명 + 태그라인) */}
            <motion.div variants={variants.word} className="text-center md:text-left">
              <div className="relative inline-block">
                {/* 광택 효과 애니메이션 */}
                <motion.span
                  aria-hidden
                  className="pointer-events-none absolute -inset-x-8 -inset-y-6"
                  style={{
                    background: BACKGROUND_STYLES.sheen,
                    transform: "skewX(-18deg)",
                    filter: "blur(1px)",
                    maskImage: "linear-gradient(#000, #000)",
                    WebkitMaskImage: "linear-gradient(#000, #000)",
                  }}
                  initial={{ x: "-30%" }}
                  animate={reduce ? { x: "0%" } : { x: ["-35%", "115%"] }}
                  transition={reduce ? {} : { duration: 1.6, delay: 0.35, ease: "easeInOut" }}
                />

                <h1 className="select-none font-['Beckman',sans-serif] font-extrabold leading-[0.88] tracking-[-0.03em]">
                  <span className="block text-slate-900 text-[clamp(2.6rem,6.4vw,5.6rem)]">
                    CARRY
                  </span>
                  <span
                    className="block bg-clip-text text-transparent text-[clamp(2.6rem,6.4vw,5.6rem)]"
                    style={{ backgroundImage: BACKGROUND_STYLES.porterText }}
                  >
                    PORTER
                  </span>
                </h1>
              </div>

              {/* 태그라인 (페이드인 애니메이션) */}
              <div className="mt-6 min-h-[2.2rem]">
                <TaglineReveal text={tagline} onDone={() => setTaglineDone(true)} />
              </div>

              {/* 로딩 인디케이터 (바운스 애니메이션) */}
              <div className="mt-10 flex justify-center md:justify-start">
                <div className="flex items-center gap-3">
                  {[0, 1, 2].map((i) => (
                    <motion.span
                      key={i}
                      className="h-2.5 w-2.5 rounded-full"
                      style={{ backgroundColor: "rgba(0,100,255,0.28)" }}
                      animate={reduce ? {} : { y: [0, -6, 0], opacity: [0.5, 1, 0.5] }}
                      transition={reduce ? {} : { duration: 0.85, repeat: Infinity, delay: i * 0.14, ease: "easeInOut" }}
                    />
                  ))}
                </div>
              </div>
            </motion.div>
          </div>

          {/* 푸터 - 저작권 */}
          <div className="mt-16 flex items-center justify-center">
            <p className="font-['Pretendard'] text-xs text-slate-400">© CARRY PORTER</p>
          </div>
        </motion.div>
      </div>
    </motion.div>
  );
};

export default SplashPage;
