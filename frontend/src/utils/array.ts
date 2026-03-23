/**
 * 배열을 랜덤하게 섞기 (Fisher-Yates 알고리즘)
 */
export const shuffleArray = <T>(array: T[]): T[] => {
    const shuffled = [...array];
    for (let i = shuffled.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    return shuffled;
};

/**
 * 랜덤 2자리 숫자 생성 (10-99)
 * correctCode와 중복되지 않도록
 */
export const generateFakeCodes = (
    correctCode: number,
    count: number,
): number[] => {
    const fakeCodes: number[] = [];
    while (fakeCodes.length < count) {
        const randomCode = Math.floor(Math.random() * 90) + 10;
        if (randomCode !== correctCode && !fakeCodes.includes(randomCode)) {
            fakeCodes.push(randomCode);
        }
    }
    return fakeCodes;
};
