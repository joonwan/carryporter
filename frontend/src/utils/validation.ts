import { z } from 'zod';

// 인증번호 발송 폼 검증 스키마
export const sendCodeSchema = z.object({
  email: z
    .string()
    .min(1, '이메일을 입력해주세요')
    .email('올바른 이메일 형식이 아닙니다'),

  password: z
    .string()
    .length(4, '비밀번호는 정확히 4자리여야 합니다')
    .regex(/^\d{4}$/, '비밀번호는 숫자 4자리여야 합니다'),

  passwordConfirm: z
    .string()
    .min(1, '비밀번호 확인을 입력해주세요'),

  agreeTerms: z
    .boolean()
    .refine((val) => val === true, {
      message: '보관 정책에 동의해주세요',
    }),

  agreePrivacy: z
    .boolean()
    .refine((val) => val === true, {
      message: '서비스 이용약관에 동의해주세요',
    }),
}).refine((data) => data.password === data.passwordConfirm, {
  message: '비밀번호가 일치하지 않습니다',
  path: ['passwordConfirm'],
});

// 타입 추출
export type SendCodeFormData = z.infer<typeof sendCodeSchema>;
