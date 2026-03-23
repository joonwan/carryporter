import { Loader2 } from "lucide-react";
import { motion } from "framer-motion";

interface TimelineStepProps {
    label: string;
    active: boolean;
    completed: boolean;
}

/**
 * 미션 타임라인 단계 표시 컴포넌트
 * 완료/진행중/대기 상태를 시각적으로 표현
 */
export const TimelineStep = ({
    label,
    active,
    completed,
}: TimelineStepProps) => (
    <motion.div
        className="flex items-center gap-4"
        initial={{ opacity: 0, x: -20 }}
        animate={{ opacity: 1, x: 0 }}
        transition={{ duration: 0.3 }}
    >
        <motion.div
            className={`w-10 h-10 rounded-full flex items-center justify-center font-semibold transition-all ${
                completed
                    ? "bg-toss-green text-white shadow-lg shadow-toss-green/50"
                    : active
                      ? "bg-toss-blue-500 text-white animate-pulse-soft animate-pulse-glow"
                      : "bg-gray-200 text-gray-400"
            }`}
            initial={completed ? { scale: 0 } : { scale: 1 }}
            animate={{ scale: 1 }}
            transition={{ type: "spring", stiffness: 200, damping: 15 }}
        >
            {completed ? (
                <motion.span
                    initial={{ scale: 0, rotate: -180 }}
                    animate={{ scale: 1, rotate: 0 }}
                    transition={{ type: "spring", stiffness: 200, damping: 10 }}
                >
                    ✓
                </motion.span>
            ) : active ? (
                <Loader2 className="w-5 h-5 animate-spin" />
            ) : (
                "○"
            )}
        </motion.div>
        <div className="flex-1 flex items-center gap-2">
            <span
                className={`font-medium ${
                    active
                        ? "text-gray-900 text-base font-bold"
                        : completed
                          ? "text-gray-700 font-semibold"
                          : "text-gray-600"
                }`}
            >
                {label}
            </span>
            {active && (
                <motion.span
                    className="inline-block w-1.5 h-1.5 bg-toss-blue-500 rounded-full"
                    animate={{ opacity: [1, 0.3, 1] }}
                    transition={{
                        duration: 1.5,
                        repeat: Infinity,
                        ease: "easeInOut",
                    }}
                />
            )}
        </div>
    </motion.div>
);
