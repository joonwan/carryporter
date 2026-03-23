import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"

import { cn } from "@/lib/utils"

const badgeVariants = cva(
  "inline-flex items-center rounded-full border px-2.5 py-0.5 text-xs font-semibold transition-colors focus:outline-none focus:ring-2 focus:ring-ring focus:ring-offset-2",
  {
    variants: {
      variant: {
        default:
          "border-transparent bg-primary-blue text-white shadow hover:bg-primary-blue/80",
        secondary:
          "border-transparent bg-gray-200 text-gray-900 hover:bg-gray-300",
        destructive:
          "border-transparent bg-sub-red text-white shadow hover:bg-sub-red/80",
        outline: "text-gray-700 border-gray-300",
        success:
          "border-transparent bg-sub-cyan text-gray-900 shadow hover:bg-sub-cyan/80",
        warning:
          "border-transparent bg-sub-orange text-white shadow hover:bg-sub-orange/80",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

export interface BadgeProps
  extends React.HTMLAttributes<HTMLDivElement>,
    VariantProps<typeof badgeVariants> {}

function Badge({ className, variant, ...props }: BadgeProps) {
  return (
    <div className={cn(badgeVariants({ variant }), className)} {...props} />
  )
}

export { Badge, badgeVariants }
