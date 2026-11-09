/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#EAF5EE',
          100: '#D2EAD9',
          600: '#217A4D',
          700: '#185C3A',
          800: '#12452B',
          900: '#0E3622',
        },
        cream: {
          50: '#FCFAF0',
          100: '#F8F0C6',
          200: '#F0E6A2',
          500: '#E3C04A',
        },
        neutral: {
          900: '#17211C',
          600: '#4A5A52',
          500: '#7A867E',
          300: '#D5DAC8',
          100: '#F2F4EE',
        },
        success: {
          50: '#E6F5EC',
          600: '#187A43',
        },
        warning: {
          50: '#FFE9C7',
          600: '#8A5000',
        },
        danger: {
          50: '#FDECEA',
          600: '#B3261E',
        },
        info: {
          50: '#E6F2FA',
          600: '#1B6FA8',
        },
        closed: {
          200: '#E4E7E1',
          600: '#5C665F',
        }
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', '"Segoe UI"', 'Roboto', 'sans-serif'],
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        pulseGlow: {
          '0%, 100%': { opacity: '0.6', transform: 'scale(1)' },
          '50%': { opacity: '1', transform: 'scale(1.05)' },
        },
        fadeIn: {
          '0%': { opacity: '0', transform: 'translateY(12px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(24px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        zoomSlow: {
          '0%': { transform: 'scale(1)' },
          '100%': { transform: 'scale(1.08)' },
        },
        shimmer: {
          '100%': { transform: 'translateX(100%)' },
        },
      },
      animation: {
        float: 'float 5s ease-in-out infinite',
        'pulse-glow': 'pulseGlow 3s ease-in-out infinite',
        'fade-in': 'fadeIn 0.5s cubic-bezier(0.16, 1, 0.3, 1) forwards',
        'slide-up': 'slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards',
        'zoom-slow': 'zoomSlow 25s ease-in-out infinite alternate',
        shimmer: 'shimmer 2s infinite',
      },
    },
  },
  plugins: [],
}
