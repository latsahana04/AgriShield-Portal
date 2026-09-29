/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./src/**/*.{js,jsx,ts,tsx}",
    "./public/index.html"
  ],
  theme: {
    extend: {
      colors: {
        forest: {
          DEFAULT: '#0D3C26',
          light: '#145334',
          dark: '#082517'
        },
        amber: {
          custom: '#D97706',
          light: '#FEF3E2'
        },
        safe: {
          DEFAULT: '#10B981',
          light: '#E7F9F1',
          dark: '#067A55'
        },
        danger: {
          DEFAULT: '#EF4444',
          light: '#FEECEC',
          dark: '#C62828'
        }
      }
    },
  },
  plugins: [],
}
