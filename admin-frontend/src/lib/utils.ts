import { type ClassValue, clsx } from "clsx"
import { twMerge } from "tailwind-merge"

/**
 * Tailwind CSS 클래스를 병합하는 유틸리티 함수
 * 중복 클래스를 자동으로 처리함
 */
export function cn(...inputs: ClassValue[]) {
    return twMerge(clsx(inputs))
}
