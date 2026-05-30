/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {
      colors: {
        primary: '#2D2F8F',
        'primary-dark': '#21236B',
        'primary-light': '#3D3FA8',
        accent: '#FFFFFF',
        'accent-muted': '#FFFFFF99',
        'bg-base': '#F4F5FA',
        'bg-card': '#FFFFFF',
        'bg-sidebar': '#2D2F8F',
        'bg-active': '#FFFFFF1A',
        success: '#22C55E',
        warning: '#F59E0B',
        danger: '#EF4444',
        info: '#3B82F6',
        neutral: '#6B7280',
        'text-primary': '#111827',
        'text-secondary': '#6B7280',
        'text-on-primary': '#FFFFFF',
        'text-link': '#2D2F8F',
      },
      fontFamily: {
        sans: ['Avenir Next', 'Nunito Sans', 'Calibri', 'sans-serif'],
      },
    },
  },
  plugins: [],
}
