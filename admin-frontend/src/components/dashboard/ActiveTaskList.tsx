// src/components/dashboard/ActiveTaskList.tsx
import { motion } from 'framer-motion'
import { Activity, Bot, ChevronRight } from 'lucide-react'

interface ActiveTaskListProps {
  robots: any[];
  count: number;
  onRobotClick: (robot: any) => void;
}

export default function ActiveTaskList({ robots, count, onRobotClick }: ActiveTaskListProps) {
  return (
    <section className="bg-white border border-slate-200 rounded-lg p-5 flex-1 min-h-[200px] flex flex-col shadow-sm">
      <div className="flex items-center justify-between mb-4">
        <div className="flex items-center gap-2 text-xs font-extrabold text-slate-800">
          <Activity className="w-4 h-4 text-amber-500" /> ACTIVE_TASKS
        </div>
        <span className="text-[10px] bg-amber-50 text-amber-700 px-2 py-0.5 rounded-full border border-amber-200 font-bold">
          {count} ACTIVE
        </span>
      </div>

      <div className="flex-1 overflow-y-auto space-y-2 pr-1 custom-scrollbar">
        {robots.length > 0 ? (
          robots.map((robot) => (
            <motion.div
              key={robot.id}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              className="group flex items-center justify-between p-3 bg-slate-50 border border-slate-200 rounded-lg hover:bg-white hover:border-amber-400 hover:shadow-md transition-all cursor-pointer"
              onClick={() => onRobotClick(robot)}
            >
              <div className="flex items-center gap-3">
                <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse shadow-sm" />
                <div>
                  <div className="text-xs font-bold text-slate-800 group-hover:text-amber-600 transition-colors">
                    {robot.name}
                  </div>
                  <div className="text-[10px] text-slate-400 font-mono font-medium">
                    TASK: #{Math.floor(Math.random() * 9000) + 1000}
                  </div>
                </div>
              </div>
              <div className="text-right">
                <div className="text-[10px] font-mono font-bold text-slate-500">
                  {robot.battery}% BAT
                </div>
                <ChevronRight size={14} className="ml-auto text-slate-300 group-hover:text-amber-500" />
              </div>
            </motion.div>
          ))
        ) : (
          <div className="h-full flex flex-col items-center justify-center text-slate-400 space-y-2 opacity-70">
            <Bot size={32} className="text-slate-300" />
            <p className="text-xs font-mono font-medium">NO ACTIVE TASKS</p>
          </div>
        )}
      </div>
    </section>
  )
}