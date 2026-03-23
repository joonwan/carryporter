import { Card } from "@/components/ui/card";
import { ProgressBar } from "@/components/mission/ProgressBar";
import { TimelineStep } from "@/components/mission/TimelineStep";
import { AnimatePresence, motion } from "framer-motion";
import { isStepCompleted } from "@/domain/mission/stateMachine";
import type { MissionStatus } from "@/types/mission.types";

interface MissionTimelineProps {
    status: MissionStatus;
    progressStep: number;
}

export const MissionTimeline = ({
    status,
    progressStep,
}: MissionTimelineProps) => {
    return (
        <Card className="p-5">
            <h3 className="text-heading-3 mb-4 flex items-center gap-2">
                <svg
                    className="w-5 h-5 text-primary-blue"
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                >
                    <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2"
                    />
                </svg>
                진행 상황
            </h3>

            <div className="mb-6">
                <ProgressBar currentStep={progressStep} totalSteps={5} />
            </div>

            <AnimatePresence mode="wait">
                <motion.div
                    key={status}
                    className="space-y-4"
                    initial={{ opacity: 0 }}
                    animate={{ opacity: 1 }}
                    exit={{ opacity: 0 }}
                    transition={{ duration: 0.3 }}
                >
                    <TimelineStep
                        label="요청됨"
                        active={status === "REQUESTED"}
                        completed={isStepCompleted(status, "REQUESTED")}
                    />
                    <TimelineStep
                        label="로봇 배정"
                        active={status === "ASSIGNED"}
                        completed={isStepCompleted(status, "ASSIGNED")}
                    />
                    <TimelineStep
                        label="이동 중"
                        active={status === "MOVING"}
                        completed={isStepCompleted(status, "MOVING")}
                    />
                    <TimelineStep
                        label="도착"
                        active={status === "ARRIVED"}
                        completed={isStepCompleted(status, "ARRIVED")}
                    />
                    <TimelineStep
                        label="완료"
                        active={status === "FINISHED"}
                        completed={status === "FINISHED"}
                    />
                </motion.div>
            </AnimatePresence>
        </Card>
    );
};
