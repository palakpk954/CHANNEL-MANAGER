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
        sans: ['Inter', 'system-ui', '-apple-system', 'sans-serif'],
      }
    },
  },
  plugins: [],
}
