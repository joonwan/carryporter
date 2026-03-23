interface WelcomeSectionProps {
    email: string | undefined;
}

const WelcomeSection = ({ email }: WelcomeSectionProps) => {
    // 이메일에서 @ 앞부분만 추출
    const userName = email?.split('@')[0] || '여행자';

    return (
        <div className="mb-4 animate-fade-in-up">
            <h2 className="text-heading-2 mb-1">안녕하세요 👋</h2>
            <p className="text-body-small">{userName}님</p>
        </div>
    );
};

export default WelcomeSection;
