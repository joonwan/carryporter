import { Checkbox } from "@/components/ui/checkbox";
import { Controller } from "react-hook-form";
import type { Control, FieldErrors } from "react-hook-form";
import type { SendCodeFormData } from "../../utils/validation";

interface TermsCheckboxProps {
  control: Control<SendCodeFormData>;
  name: "agreeTerms" | "agreePrivacy";
  label: string;
  errors: FieldErrors<SendCodeFormData>;
}

/**
 * 약관 동의 체크박스 컴포넌트
 * react-hook-form의 Controller를 사용하여 boolean 값 관리
 */
export function TermsCheckbox({
  control,
  name,
  label,
  errors,
}: TermsCheckboxProps) {
  const error = errors[name];

  return (
    <div className="space-y-2">
      <Controller
        name={name}
        control={control}
        render={({ field }) => (
          <div className="flex items-center space-x-2">
            <Checkbox
              id={name}
              checked={field.value}
              onCheckedChange={field.onChange}
            />
            <label
              htmlFor={name}
              className="text-sm font-medium leading-none peer-disabled:cursor-not-allowed peer-disabled:opacity-70"
            >
              {label}
            </label>
          </div>
        )}
      />
      {error && <p className="text-sm text-red-500">{error.message}</p>}
    </div>
  );
}
