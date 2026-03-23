import { Input } from "@/components/ui/input";
import type { UseFormRegister, FieldErrors } from "react-hook-form";
import type { SendCodeFormData } from "../../utils/validation";

interface PasswordInputFieldProps {
  register: UseFormRegister<SendCodeFormData>;
  errors: FieldErrors<SendCodeFormData>;
  name: "password" | "passwordConfirm";
  label: string;
  placeholder: string;
}

/**
 * 4자리 숫자 비밀번호 입력 필드 컴포넌트
 * react-hook-form과 통합되어 유효성 검사 및 에러 표시 기능 제공
 */
export function PasswordInputField({
  register,
  errors,
  name,
  label,
  placeholder,
}: PasswordInputFieldProps) {
  const error = errors[name];

  return (
    <div className="space-y-2">
      <label
        htmlFor={name}
        className="block text-sm font-medium text-gray-700"
      >
        {label}
      </label>
      <Input
        id={name}
        type="password"
        inputMode="numeric"
        maxLength={4}
        placeholder={placeholder}
        {...register(name)}
        className={error ? "border-red-500" : ""}
      />
      {error && <p className="text-sm text-red-500">{error.message}</p>}
    </div>
  );
}
