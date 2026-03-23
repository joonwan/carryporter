import { Card } from '@/components/ui/card';

interface RobotInfoCardProps {
  robotCode: string;
}

export const RobotInfoCard = ({ robotCode }: RobotInfoCardProps) => {
  return (
    <Card className="p-5">
      <h3 className="text-heading-3 mb-3 flex items-center gap-2">
        <svg className="w-5 h-5 text-sub-cyan" fill="none" viewBox="0 0 24 24" stroke="currentColor">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 3v2m6-2v2M9 19v2m6-2v2M5 9H3m2 6H3m18-6h-2m2 6h-2M7 19h10a2 2 0 002-2V7a2 2 0 00-2-2H7a2 2 0 00-2 2v10a2 2 0 002 2zM9 9h6v6H9V9z" />
        </svg>
        배정 로봇
      </h3>
      <div className="bg-gray-100 rounded-xl p-4 flex items-center gap-4">
        <div className="w-12 h-12 bg-gradient-to-br from-toss-blue-500 to-sub-cyan rounded-xl flex items-center justify-center">
          <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24">
            <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z" />
          </svg>
        </div>
        <div>
          <p className="text-caption">로봇 코드</p>
          <p className="text-xl font-bold text-gray-900">{robotCode}</p>
        </div>
      </div>
    </Card>
  );
};
