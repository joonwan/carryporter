/**
 * Base64 문자열을 File 객체로 변환
 * react-webcam의 getScreenshot()은 base64 문자열을 반환하므로
 * 백엔드 API에 전송하기 위해 File 객체로 변환이 필요함
 *
 * @param base64String - data:image/jpeg;base64,... 형식의 문자열
 * @param filename - 파일 이름 (기본값: "ticket.jpg")
 * @returns File 객체
 */
export const base64ToFile = (base64String: string, filename = 'ticket.jpg'): File => {
  // data:image/jpeg;base64, 부분 제거
  const base64Data = base64String.split(',')[1];

  // Base64를 바이너리 문자열로 디코딩
  const binaryString = atob(base64Data);

  // 바이너리 문자열을 Uint8Array로 변환
  const bytes = new Uint8Array(binaryString.length);
  for (let i = 0; i < binaryString.length; i++) {
    bytes[i] = binaryString.charCodeAt(i);
  }

  // Blob 생성
  const blob = new Blob([bytes], { type: 'image/jpeg' });

  // File 객체로 변환
  return new File([blob], filename, { type: 'image/jpeg' });
};

/**
 * 시간 문자열 포맷팅
 * @param time - "21:20" 형식의 시간 문자열 (null/undefined 가능)
 * @returns "21:20" (그대로 반환, 필요시 추가 포맷팅 가능) 또는 null인 경우 "--:--"
 */
export const formatTime = (time?: string | null): string => {
  // null이나 undefined인 경우 기본값 반환
  if (!time) {
    return '--:--';
  }

  return time;
};

/**
 * 도시 이름 포맷팅 (첫 글자만 대문자, 나머지 소문자)
 * @param city - "ROME" 형식의 도시 이름 (null/undefined 가능)
 * @returns "Rome" 또는 null인 경우 "-"
 */
export const formatCityName = (city?: string | null): string => {
  // null이나 undefined인 경우 기본값 반환
  if (!city) {
    return '-';
  }

  // 빈 문자열인 경우 기본값 반환
  if (city.trim() === '') {
    return '-';
  }

  return city.charAt(0).toUpperCase() + city.slice(1).toLowerCase();
};
