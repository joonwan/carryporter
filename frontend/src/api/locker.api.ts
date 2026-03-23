import apiClient from './axios';
import type { UserStoringLockerResponse, CurrentLocker } from '../types/locker.types';

/**
 * 사용자가 현재 사용 중인 사물함 조회
 *
 * @returns 사물함 정보 또는 null (사물함 없음)
 * @throws API 호출 실패 시 에러
 */
export const getUserStoringLocker = async (): Promise<CurrentLocker | null> => {
  const { data } = await apiClient.get<UserStoringLockerResponse>(
    '/api/me/lockers/storing'
  );

  // 사물함이 없는 경우 (lockerCode 또는 updatedAt이 null)
  if (!data.lockerCode || !data.updatedAt) {
    return null;
  }

  return {
    lockerCode: data.lockerCode,  // 백엔드 값 그대로 사용
    updatedAt: data.updatedAt,    // 원본 형식 유지
  };
};
