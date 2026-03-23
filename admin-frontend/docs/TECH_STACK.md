# 기술 스택 상세 가이드

> CARRY PORTER가 사용하는 최신 기술들의 특징과 선택 이유

---

## ✅ 버전 확인

### 현재 사용 중인 버전 (2026년 1월 기준)

| 기술 | 버전 | 릴리즈 | 상태 |
|------|------|--------|------|
| **React** | 19.2.0 | 2024년 12월 | ✅ 최신 LTS |
| **TypeScript** | 5.9.3 | 2024년 | ✅ 최신 |
| **Vite** | 7.3.1 | 2025년 | ✅ 최신 |
| **Tailwind CSS** | 4.1.18 | 2024년 10월 | ✅ 최신 v4 |
| **React Router** | 7.13.0 | 2024년 | ✅ 최신 |
| **Zustand** | 5.0.10 | 2024년 | ✅ 최신 |

**결론**: 모든 라이브러리가 최신 버전입니다! 🎉

---

## React 19의 새로운 기능

### 1. React Compiler (자동 최적화)

**이전 (React 18)**:
```typescript
// useMemo, useCallback을 수동으로 추가해야 함
const expensiveValue = useMemo(() => {
  return computeExpensiveValue(a, b);
}, [a, b]);

const handleClick = useCallback(() => {
  console.log('clicked');
}, []);
```

**React 19**:
```typescript
// 자동으로 최적화됨!
const expensiveValue = computeExpensiveValue(a, b);

const handleClick = () => {
  console.log('clicked');
};
```

**장점**:
- 코드가 간결해짐
- 실수 방지 (의존성 배열 누락 등)
- 성능 향상

---

### 2. Server Components (RSC)

**설명**: 서버에서 렌더링되는 컴포넌트

```typescript
// app/page.tsx (Server Component)
async function HomePage() {
  const data = await fetch('https://api.example.com/data');
  return <div>{data.title}</div>;
}
```

**장점**:
- 번들 크기 감소
- SEO 향상
- 빠른 초기 로딩

**우리 프로젝트**: 아직 사용 안 함 (추후 고려)

---

### 3. Actions

**설명**: 폼 제출을 간단하게 처리

```typescript
// 이전
const handleSubmit = async (e) => {
  e.preventDefault();
  const formData = new FormData(e.target);
  await submitForm(formData);
};

// React 19
<form action={submitForm}>
  <input name="email" />
  <button type="submit">제출</button>
</form>
```

**우리 프로젝트**: 아직 사용 안 함 (React Hook Form 사용 중)

---

### 4. Document Metadata

**설명**: `<title>`, `<meta>` 태그를 컴포넌트 내부에서 직접 작성

```typescript
function AboutPage() {
  return (
    <>
      <title>소개 - CARRY PORTER</title>
      <meta name="description" content="짐 운반 서비스" />
      <h1>소개</h1>
    </>
  );
}
```

**우리 프로젝트**: 추후 적용 가능

---

### 5. `use` Hook

**설명**: Promise나 Context를 직접 읽을 수 있음

```typescript
// Promise
const data = use(fetchData());

// Context
const theme = use(ThemeContext);
```

**우리 프로젝트**: 아직 사용 안 함

---

## Vite 7의 특징

### 1. 초고속 빌드

**벤치마크**:
- 개발 서버 시작: < 1초
- HMR: < 100ms
- Production 빌드: Webpack 대비 **10배 빠름**

**이유**:
- esbuild 사용 (Go 언어로 작성)
- Native ESM 활용

---

### 2. 환경 변수

**Vite 규칙**:
- `VITE_` 접두사 필수
- `import.meta.env` 사용

```typescript
// .env.development
VITE_API_BASE_URL=http://localhost:8080

// 코드에서
const apiUrl = import.meta.env.VITE_API_BASE_URL;
```

**주의**: `VITE_` 없으면 브라우저에서 접근 불가!

---

### 3. 플러그인 시스템

**설치된 플러그인**:
- `@vitejs/plugin-react`: React 지원

**추가 가능한 플러그인**:
- `vite-plugin-pwa`: PWA 지원
- `vite-plugin-imagemin`: 이미지 최적화
- `vite-plugin-compression`: Gzip 압축

---

## Tailwind CSS v4의 변화

### 이전 버전 (v3)과 차이점

#### 1. 설정 방식 변경

**Tailwind v3**:
```javascript
// tailwind.config.js
module.exports = {
  content: ['./src/**/*.{js,jsx,ts,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#3b82f6',
      },
    },
  },
};
```

**Tailwind v4**:
```css
/* src/index.css */
@import "tailwindcss";

@theme {
  --color-primary-500: #3b82f6;
}
```

**장점**:
- JavaScript 파일 불필요
- CSS 파일에서 직접 설정
- 더 직관적

---

#### 2. PostCSS 플러그인 분리

**Tailwind v3**:
```javascript
// postcss.config.js
module.exports = {
  plugins: {
    tailwindcss: {},
  },
};
```

**Tailwind v4**:
```javascript
// postcss.config.js
module.exports = {
  plugins: {
    '@tailwindcss/postcss': {},  // 별도 패키지
  },
};
```

---

#### 3. 성능 향상

- **빌드 속도**: 50% 빠름
- **번들 크기**: 30% 감소
- **JIT 모드**: 기본 활성화

---

### 주요 클래스

```css
/* 레이아웃 */
flex, grid, block, inline
justify-center, items-center
gap-4, space-y-2

/* 크기 */
w-full, h-screen
max-w-md, min-h-0

/* 색상 */
bg-blue-600, text-white
border-gray-300

/* 간격 */
p-4 (padding: 1rem)
m-2 (margin: 0.5rem)
px-6 (padding-left/right: 1.5rem)

/* 텍스트 */
text-lg, font-bold
text-center

/* 반응형 */
sm:text-xl (640px+)
md:flex (768px+)
lg:grid (1024px+)

/* 상태 */
hover:bg-blue-700
focus:ring-2
disabled:opacity-50
```

---

## TypeScript 5.9의 특징

### 1. 더 나은 타입 추론

```typescript
// 이전: 타입 명시 필요
const items: string[] = ['a', 'b', 'c'];

// TypeScript 5.9: 자동 추론
const items = ['a', 'b', 'c']; // string[]
```

---

### 2. `satisfies` 키워드

```typescript
interface Config {
  apiUrl: string;
  timeout: number;
}

const config = {
  apiUrl: 'http://localhost:8080',
  timeout: 10000,
  extra: true,  // OK!
} satisfies Partial<Config>;
```

---

### 3. `const` 타입 파라미터

```typescript
function useState<const T>(initialValue: T) {
  // T는 literal 타입 유지
}

const [count] = useState(42); // count: 42 (not number)
```

---

## Zustand vs Redux

### 비교

| 항목 | Zustand | Redux |
|------|---------|-------|
| 번들 크기 | 1KB | 15KB |
| 보일러플레이트 | 거의 없음 | 많음 |
| 학습 곡선 | 낮음 | 높음 |
| TypeScript | 완벽 | 좋음 |
| DevTools | 있음 | 있음 |

---

### 코드 비교

**Zustand**:
```typescript
// 30줄
const useStore = create((set) => ({
  count: 0,
  increment: () => set((state) => ({ count: state.count + 1 })),
}));
```

**Redux**:
```typescript
// 100줄+
// actions, reducers, store, types, provider...
```

---

## React Router 7의 특징

### 1. Data APIs

```typescript
// loader: 페이지 로딩 전에 데이터 가져오기
export async function loader() {
  const data = await fetchData();
  return data;
}

// action: 폼 제출 처리
export async function action({ request }) {
  const formData = await request.formData();
  await submitData(formData);
  return redirect('/success');
}
```

**우리 프로젝트**: 아직 사용 안 함 (API 함수로 직접 호출)

---

### 2. Outlet Context

```typescript
// 부모
<Outlet context={{ user }} />

// 자식
const { user } = useOutletContext<{ user: User }>();
```

---

## React Hook Form vs Formik

### 비교

| 항목 | React Hook Form | Formik |
|------|----------------|--------|
| 렌더링 | 최소화 | 많음 |
| 성능 | 빠름 | 느림 |
| 번들 크기 | 9KB | 15KB |
| TypeScript | 완벽 | 좋음 |
| Validation | Yup, Zod | Yup |

---

### 성능 차이

**React Hook Form**:
- Uncontrolled Components 사용
- 필드별 re-render 없음
- 제출 시에만 검증

**Formik**:
- Controlled Components 사용
- 입력마다 re-render
- 실시간 검증

---

## Axios vs Fetch API

### 비교

| 기능 | Axios | Fetch |
|------|-------|-------|
| JSON 자동 변환 | ✅ | ❌ (수동) |
| Timeout | ✅ | ❌ |
| Interceptors | ✅ | ❌ |
| Progress | ✅ | ❌ |
| Cancel | ✅ | ✅ (AbortController) |
| 브라우저 지원 | 모든 브라우저 | 최신 브라우저 |

---

### 코드 비교

**Axios**:
```typescript
const response = await axios.get('/api/users');
console.log(response.data); // 자동 JSON 파싱
```

**Fetch**:
```typescript
const response = await fetch('/api/users');
const data = await response.json(); // 수동 JSON 파싱
```

---

## React Query vs SWR

### 비교

| 항목 | React Query | SWR |
|------|------------|-----|
| 기능 | 풍부 | 기본적 |
| 번들 크기 | 13KB | 4KB |
| DevTools | ✅ | ❌ |
| Mutation | ✅ | 제한적 |
| Infinite Query | ✅ | ✅ |

**우리 선택**: React Query (더 많은 기능)

---

### 주요 기능

**캐싱**:
```typescript
const { data } = useQuery({
  queryKey: ['users'],
  queryFn: fetchUsers,
  staleTime: 5000, // 5초간 신선
});
```

**자동 재시도**:
```typescript
const { data } = useQuery({
  queryKey: ['users'],
  queryFn: fetchUsers,
  retry: 3, // 3번 재시도
});
```

**Refetch on Window Focus**:
```typescript
// 탭 전환 후 돌아오면 자동 갱신
```

---

## 보안 모범 사례

### 1. XSS 방지

**안전**:
```typescript
<div>{user.name}</div>  // React가 자동 이스케이프
```

**위험**:
```typescript
<div dangerouslySetInnerHTML={{ __html: userInput }} />
```

---

### 2. 토큰 저장

**좋음**: 메모리 (Zustand)
```typescript
const useAuthStore = create((set) => ({
  accessToken: null, // 메모리에만 존재
}));
```

**나쁨**: LocalStorage
```typescript
localStorage.setItem('token', token); // XSS에 취약
```

**최선**: HttpOnly Cookie (백엔드 설정)

---

### 3. HTTPS

Production에서는 **반드시 HTTPS** 사용!

```nginx
# Nginx 설정 예시
server {
  listen 443 ssl;
  ssl_certificate /path/to/cert.pem;
  ssl_certificate_key /path/to/key.pem;
}
```

---

## 성능 최적화 체크리스트

- ✅ Code Splitting (React.lazy)
- ✅ Tree Shaking (Vite 자동)
- ✅ 이미지 최적화 (WebP, lazy loading)
- ✅ Gzip 압축
- ⏳ Service Worker (PWA)
- ⏳ CDN 사용
- ⏳ 서버 사이드 렌더링 (SSR)

---

## 번들 크기 분석

```bash
# 빌드
npm run build

# 번들 크기 확인
npx vite-bundle-visualizer
```

**목표**:
- 초기 로딩: < 300KB
- JS: < 200KB
- CSS: < 50KB

---

## 브라우저 지원

**지원하는 브라우저**:
- Chrome 90+
- Firefox 88+
- Safari 14+
- Edge 90+

**지원하지 않는 브라우저**:
- IE 11 (React 19는 지원 안 함)

---

## 개발 도구

### 필수

1. **React DevTools**
   - 컴포넌트 트리 검사
   - Props/State 확인
   - 성능 프로파일링

2. **Redux DevTools** (Zustand 지원)
   - 상태 변경 추적
   - Time Travel Debugging

3. **React Query DevTools**
   - 쿼리 상태 확인
   - 캐시 검사

---

### VS Code 확장

1. **ESLint**
   - 코드 품질 검사
   - 자동 수정

2. **Prettier**
   - 코드 포맷팅
   - 일관된 스타일

3. **Tailwind CSS IntelliSense**
   - 클래스명 자동완성
   - 미리보기

4. **TypeScript Error Translator**
   - 에러 메시지 한글화
   - 쉬운 이해

---

## 학습 자료

### 공식 문서

- [React 19 문서](https://react.dev/)
- [TypeScript 핸드북](https://www.typescriptlang.org/docs/)
- [Tailwind CSS](https://tailwindcss.com/)
- [Vite 가이드](https://vitejs.dev/)

### 추천 강의

- React 공식 튜토리얼
- TypeScript Deep Dive
- Tailwind Labs YouTube

### 커뮤니티

- React 한국 사용자 그룹
- TypeScript Korea
- 프론트엔드 개발자 커뮤니티

---

## 마이그레이션 가이드

### React 18 → React 19

**변경 사항**:
1. `ReactDOM.render` 제거 → `createRoot` 사용
2. 일부 Hooks API 변경
3. Strict Mode 강화

**우리 프로젝트**: 처음부터 React 19 사용 (마이그레이션 불필요)

---

### Tailwind v3 → v4

**변경 사항**:
1. `tailwind.config.js` → CSS 파일 설정
2. PostCSS 플러그인 변경
3. 일부 클래스 이름 변경

**이미 적용됨**: ✅

---

## 자주 묻는 질문

### Q: React 19를 프로덕션에서 사용해도 안전한가요?

**A**: 네! React 19는 2024년 12월에 정식 릴리즈된 LTS 버전입니다.

---

### Q: Tailwind CSS v4가 안정적인가요?

**A**: 네! 2024년 10월에 정식 릴리즈되었고, 성능이 크게 향상되었습니다.

---

### Q: TypeScript는 필수인가요?

**A**: 필수는 아니지만 **강력히 권장**합니다. 타입 안전성과 개발 생산성이 크게 향상됩니다.

---

### Q: 왜 Redux 대신 Zustand를 선택했나요?

**A**:
- 더 간단한 API
- 작은 번들 크기 (1KB vs 15KB)
- 낮은 학습 곡선
- 우리 프로젝트에는 충분한 기능

---

### Q: Server Components를 사용해야 하나요?

**A**: 아직은 아닙니다. 우리는 SPA(Single Page Application)이고, Server Components는 주로 Next.js에서 사용됩니다. 추후 SSR이 필요하면 고려할 수 있습니다.

---

## 버전 업데이트 정책

### 언제 업데이트하나?

**Major 버전**: 신중히 검토 후 (Breaking Changes)
**Minor 버전**: 새 기능 필요 시
**Patch 버전**: 즉시 (버그 수정)

### 업데이트 방법

```bash
# 최신 버전 확인
npm outdated

# 특정 패키지 업데이트
npm update react

# 모든 패키지 업데이트
npm update

# Major 버전 업데이트
npm install react@latest
```

---

## 결론

CARRY PORTER는 **2026년 1월 기준 최신 기술 스택**을 사용하고 있습니다!

- ✅ React 19 (최신 LTS)
- ✅ Vite 7 (최고 성능)
- ✅ Tailwind CSS v4 (최신 버전)
- ✅ TypeScript 5.9 (최신)
- ✅ 최신 라이브러리들

**앞으로 추가할 것**:
- PWA 지원
- SSR (필요 시)
- 더 많은 테스트
- CI/CD 파이프라인

**계속 최신 상태 유지!** 🚀
