import { useState } from "react";

interface TiltEffectOptions {
    maxRotation?: number;
    scale?: number;
    perspective?: number;
}

/**
 * 3D 틸트 효과 훅 — 마우스 움직임에 따라 카드가 기울어지는 효과
 */
export const useTiltEffect = (options: TiltEffectOptions = {}) => {
    const { maxRotation = 16, scale = 1.02, perspective = 1000 } = options;

    const [tiltStyle, setTiltStyle] = useState<React.CSSProperties>({});

    const handleMouseMove = (e: React.MouseEvent<HTMLDivElement>) => {
        const card = e.currentTarget;
        const rect = card.getBoundingClientRect();
        const x = (e.clientX - rect.left) / rect.width;
        const y = (e.clientY - rect.top) / rect.height;
        const centerX = x - 0.5;
        const centerY = y - 0.5;
        const rotateX = centerY * -maxRotation;
        const rotateY = centerX * maxRotation;

        setTiltStyle({
            transform: `perspective(${perspective}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) scale3d(${scale}, ${scale}, ${scale})`,
        });
    };

    const handleMouseLeave = () => {
        setTiltStyle({
            transform: `perspective(${perspective}px) rotateX(0deg) rotateY(0deg) scale3d(1, 1, 1)`,
        });
    };

    return { tiltStyle, handleMouseMove, handleMouseLeave };
};
