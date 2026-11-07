import { ButtonHTMLAttributes, ReactNode } from "react";

type Variant = "brutal" | "swiss" | "ghost";

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  icon?: ReactNode;
  fullWidth?: boolean;
}

const base =
  "inline-flex items-center justify-center gap-2 font-medium transition-transform duration-100 disabled:opacity-40 disabled:pointer-events-none";

const variants: Record<Variant, string> = {
  // Neo-Brutalism: primary, attention-worthy actions
  brutal:
    "bg-vault-accent text-vault-text border-2 border-vault-border rounded-brutal px-5 py-2.5 text-sm shadow-brutal active:shadow-none active:translate-x-[3px] active:translate-y-[3px]",
  // Swiss Minimalism: quiet, structural secondary actions
  swiss:
    "bg-vault-surface text-vault-text border border-vault-border/70 rounded-swiss px-4 py-2 text-sm hover:bg-vault-secondary",
  // Minimal text-only action
  ghost:
    "text-vault-text/70 hover:text-vault-text text-sm px-2 py-1",
};

export function Button({
  variant = "swiss",
  icon,
  fullWidth,
  className = "",
  children,
  ...props
}: ButtonProps) {
  return (
    <button
      className={`${base} ${variants[variant]} ${fullWidth ? "w-full" : ""} ${className}`}
      {...props}
    >
      {icon}
      {children}
    </button>
  );
}
