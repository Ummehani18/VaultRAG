import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./app/**/*.{ts,tsx}",
    "./components/**/*.{ts,tsx}",
  ],

  theme: {
    extend: {
      colors: {
        vault: {
          bg: "#F7F6F2",
          surface: "#FFFFFF",
          text: "#171717",
          border: "#171717",
          accent: "#D7FF3F",
          secondary: "#E8E6DF",
          muted: "#8A8778",
          warning: "#F0A937",
          "warning-bg": "#FBEBD1",
          error: "#C4432B",
          "error-bg": "#F3E1DA",
        },
      },

      fontFamily: {
        sans: ["var(--font-sans)", "system-ui", "sans-serif"],
        serif: ["var(--font-serif)", "Georgia", "serif"],
        mono: ["var(--font-mono)", "monospace"],
      },

      boxShadow: {
        brutal: "5px 5px 0 0 #171717",
        "brutal-sm": "3px 3px 0 0 #171717",
        "brutal-accent": "5px 5px 0 0 #171717",
      },

      borderRadius: {
        swiss: "10px",
        brutal: "3px",
      },

      maxWidth: {
        measure: "68ch",
      },
    },
  },

  plugins: [],
};

export default config;