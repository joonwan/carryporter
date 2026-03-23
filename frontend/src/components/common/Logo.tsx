import { useState } from 'react';
import { cn } from '@/lib/utils';

interface LogoProps {
  size?: 'sm' | 'md' | 'lg';
  variant?: 'default' | 'white';
  className?: string;
}

export const Logo = ({
  size = 'md',
  variant = 'default',
  className
}: LogoProps) => {
  const [error, setError] = useState(false);

  const sizeClasses = {
    sm: 'w-6 h-6',
    md: 'w-8 h-8',
    lg: 'w-10 h-10',
  };

  const filterClasses = {
    default: '',
    white: 'brightness-0 invert',
  };

  return (
    <img
      src="/images/logo.png"
      alt="CARRY PORTER Logo"
      className={cn(
        sizeClasses[size],
        filterClasses[variant],
        error && 'hidden',
        className
      )}
      onError={() => setError(true)}
    />
  );
};
