import { useState, useEffect } from 'react';
import { useMissionStore } from '@/store/missionStore';
import { getUserStoringLocker } from '@/api/locker.api';

export const useLockerData = () => {
  const { currentLocker, setCurrentLocker } = useMissionStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadLocker = async () => {
      try {
        setIsLoading(true);
        setError(null);

        const locker = await getUserStoringLocker();
        setCurrentLocker(locker);
      } catch (error: any) {
        console.error('사물함 조회 실패:', error);

        if (error.response?.status === 401) return;

        setError('사물함 정보를 불러올 수 없습니다.');
        setCurrentLocker(null);
      } finally {
        setIsLoading(false);
      }
    };

    loadLocker();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return { currentLocker, isLoading, error };
};
