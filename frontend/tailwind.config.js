/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        canvas: "#f6f2ea",
        ink: "#171717",
        mist: "#dfd2bf",
        clay: "#c07239",
        pine: "#28453f",
      },
      boxShadow: {
        float: "0 24px 60px rgba(31, 27, 21, 0.12)",
      },
      fontFamily: {
        sans: ["Avenir Next", "Noto Sans TC", "Segoe UI", "sans-serif"],
      },
    },
  },
  plugins: [],
};
