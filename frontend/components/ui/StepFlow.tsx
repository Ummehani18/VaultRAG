import { ReactNode } from "react";
import { Check } from "lucide-react";

export type StepStatus = "done" | "active" | "upcoming";

export function StepRow({
  number,
  status,
  title,
  meta,
  last = false,
  children,
}: {
  number: number;
  status: StepStatus;
  title: string;
  meta?: string;
  last?: boolean;
  children?: ReactNode;
}) {
  return (
    <div className="flex gap-4">
      {/* Rail: number circle + connecting line */}
      <div className="flex flex-col items-center">
        <div
          className={[
            "w-8 h-8 shrink-0 rounded-full flex items-center justify-center text-sm font-medium transition-colors",
            status === "done" && "bg-vault-text text-vault-bg",
            status === "active" &&
              "bg-vault-accent text-vault-text border-2 border-vault-border",
            status === "upcoming" &&
              "border border-vault-border/25 text-vault-muted bg-vault-surface",
          ]
            .filter(Boolean)
            .join(" ")}
        >
          {status === "done" ? <Check size={15} /> : number}
        </div>
        {!last && (
          <div
            className={`w-px flex-1 mt-1 ${
              status === "done" ? "bg-vault-text" : "bg-vault-border/15"
            }`}
          />
        )}
      </div>

      {/* Content */}
      <div className={`flex-1 min-w-0 ${last ? "" : "pb-8"}`}>
        <div className="flex items-baseline justify-between gap-3 mb-2">
          <p
            className={`text-sm font-medium ${
              status === "upcoming" ? "text-vault-muted" : "text-vault-text"
            }`}
          >
            {title}
          </p>
          {meta && <p className="text-xs text-vault-muted shrink-0">{meta}</p>}
        </div>
        {children}
      </div>
    </div>
  );
}
