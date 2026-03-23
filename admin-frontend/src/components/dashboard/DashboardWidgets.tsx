import { motion } from 'framer-motion'
import { Activity, Users } from 'lucide-react'
import { cn } from '@/lib/utils'

// --- 🧱 통계 카드 ---
export function StatusCard({ label, value, valueColor = "text-slate-900", icon, iconColor = "text-slate-400" }: any) {
  return (
    <div className="p-4 bg-white border border-slate-200 rounded-lg shadow-sm flex flex-col justify-between h-24 relative overflow-hidden group hover:border-cyan-400/50 transition-colors">
      <div className={cn("absolute top-2 right-2 p-1 opacity-20 group-hover:scale-110 transition-transform duration-500", iconColor)}>
        {icon}
      </div>
      <div className="text-[10px] font-bold font-mono text-slate-500 uppercase tracking-tight z-10">{label}</div>
      <div className={cn("text-3xl font-black italic tracking-tighter z-10", valueColor)}>
        {value}
        <span className="text-xs font-medium not-italic text-slate-400 ml-1">ea</span>
      </div>
      <div className={cn("absolute bottom-0 left-0 h-1 w-full opacity-30", valueColor.replace('text-', 'bg-'))} />
    </div>
  )
}
