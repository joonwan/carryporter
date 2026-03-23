import { BrowserRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import AppRoutes from './routes';
import { useSessionRestore } from './hooks/useSessionRestore';

// React Query 클라이언트 생성
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: 1,
    },
  },
});

// 세션 복원 래퍼 컴포넌트
function SessionProvider({ children }: { children: React.ReactNode }) {
  const { isInitialized } = useSessionRestore();

  // 세션 복원 중 로딩 표시
  if (!isInitialized) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-[#0064FF] to-[#4DA3FF] flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-white/30 border-t-white rounded-full animate-spin mx-auto mb-4" />
          <p className="text-white/80 text-sm">로딩 중...</p>
        </div>
      </div>
    );
  }

  return <>{children}</>;
}

function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <SessionProvider>
          <AppRoutes />
        </SessionProvider>
      </BrowserRouter>
    </QueryClientProvider>
  );
}

export default App;
