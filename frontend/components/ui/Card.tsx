import { HTMLAttributes, ReactNode } from "react";

export function SwissCard({
  children,
  className = "",
  ...props
}: HTMLAttributes<HTMLDivElement>) {
  return (
    <div
      className={`bg-vault-surface border border-vault-border/15 rounded-swiss ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function BrutalCard({
  children,
  tone = "default",
  className = "",
  ...props
}: HTMLAttributes<HTMLDivElement> & { tone?: "default" | "warning" | "error" }) {
  const toneClasses: Record<string, string> = {
    default: "bg-vault-surface",
    warning: "bg-vault-warning-bg",
    error: "bg-vault-error-bg",
  };
  return (
    <div
      className={`${toneClasses[tone]} border-2 border-vault-border rounded-brutal shadow-brutal ${className}`}
      {...props}
    >
      {children}
    </div>
  );
}

export function EditorialBlock({
  eyebrow,
  title,
  children,
}: {
  eyebrow?: string;
  title?: string;
  children: ReactNode;
}) {
  return (
    <div>
      {eyebrow && (
        <p className="text-xs tracking-wide text-vault-muted mb-1 font-mono">{eyebrow}</p>
      )}
      {title && (
        <h2 className="font-serif text-2xl leading-snug mb-3">{title}</h2>
      )}
      <div className="prose-vault font-serif text-[17px] text-vault-text/90">{children}</div>
    </div>
  );
}
