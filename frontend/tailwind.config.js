/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: [
          'Inter',
          'Noto Sans Thai',
          'Noto Sans Myanmar',
          'system-ui',
          'sans-serif',
        ],
      },
      colors: {
        ink: {
          950: '#070d16',
          900: '#0b1322',
          800: '#101a2e',
          700: '#17233b',
        },
      },
      boxShadow: {
        glow: '0 0 40px -12px rgba(52, 211, 153, 0.35)',
      },
    },
  },
  plugins: [],
}
