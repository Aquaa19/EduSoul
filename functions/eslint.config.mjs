import js from "@eslint/js";
import globals from "globals";
import tseslint from "typescript-eslint";
import { defineConfig } from "eslint/config";

export default defineConfig([
  {
    files: ["**/*.{js,mjs,cjs,ts,mts,cts}"],
    plugins: { js },
    extends: ["js/recommended"],
    languageOptions: {
      globals: globals.node
    },
    // Remove 'rules' block from here
  },
  ...tseslint.configs.recommended, // This extends recommended TypeScript rules
  {
    // This section disables no-require-imports for compiled JS files (keep this)
    files: ["lib/**/*.js", "lib/**/*.cjs"],
    rules: {
      "@typescript-eslint/no-require-imports": "off"
    }
  },
  // Add a new configuration object at the very end for global overrides
  {
    files: ["**/*.ts"], // Target only TypeScript files where this rule is problematic
    rules: {
      "@typescript-eslint/no-explicit-any": "off" // <-- Force this rule off
    }
  }
]);