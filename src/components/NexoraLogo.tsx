import React from 'react';

interface NexoraLogoProps {
  size?: number;
  showWordmark?: boolean;
  variant?: 'dark' | 'light' | 'minimal';
  accentColor?: string;
  secondaryColor?: string;
}

/**
 * NEXORA CLIENT — Original Geometric Brand Identity.
 * Designed for high legibility at small icon scales (16x16 mcmod icon) up to full HUD headers,
 * with zero heavy textures or external image dependencies.
 */
export const NexoraLogo: React.FC<NexoraLogoProps> = ({
  size = 36,
  showWordmark = true,
  variant = 'dark',
  accentColor = '#00A8FF',
  secondaryColor = '#A371F7',
}) => {
  const textColor = variant === 'light' ? '#0B0E14' : '#E6EDF3';
  const subColor = variant === 'light' ? '#4E5969' : '#8B949E';

  return (
    <div className="inline-flex items-center gap-3 select-none" dir="ltr">
      <svg
        width={size}
        height={size}
        viewBox="0 0 64 64"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-label="NEXORA Geometric Emblem"
        className="shrink-0"
      >
        {/* Outer Precision Hexagonal Shield */}
        <polygon
          points="32,4 57,18 57,46 32,60 7,46 7,18"
          fill={variant === 'light' ? '#F3F6FA' : '#111621'}
          stroke={accentColor}
          strokeWidth="2.75"
          strokeLinejoin="round"
        />
        {/* Inner Speed Facet */}
        <polygon
          points="32,10 51,21 51,43 32,54 13,43 13,21"
          fill="none"
          stroke={secondaryColor}
          strokeWidth="1.2"
          strokeOpacity="0.55"
        />
        {/* Geometric N-Blade Vector */}
        <path
          d="M21 44V20L32 33L43 20V44"
          stroke={accentColor}
          strokeWidth="4"
          strokeLinecap="square"
          strokeLinejoin="miter"
        />
        <path
          d="M21 20L43 44"
          stroke={secondaryColor}
          strokeWidth="2.5"
          strokeLinecap="round"
        />
        {/* Core Precision Apex Node */}
        <circle cx="32" cy="32" r="3" fill="#E6EDF3" />
      </svg>

      {showWordmark && (
        <div className="flex flex-col leading-none">
          <span
            className="font-bold tracking-wider text-base md:text-lg whitespace-nowrap"
            style={{ color: textColor, fontFamily: "'Plus Jakarta Sans', sans-serif" }}
          >
            NEXORA
          </span>
          {variant !== 'minimal' && (
            <span
              className="text-[11px] font-mono tracking-widest whitespace-nowrap mt-0.5"
              style={{ color: subColor }}
            >
              FORGE 1.8.9
            </span>
          )}
        </div>
      )}
    </div>
  );
};
