import { DocStatus } from "@/lib/mock-data";

const styles: Record<DocStatus, string> = {
  Ready: "bg-vault-secondary text-vault-text",
  Processing: "bg-vault-warning-bg text-vault-warning border border-vault-warning/40",
  Uploading: "bg-vault-secondary text-vault-muted",
  Failed: "bg-vault-error-bg text-vault-error border border-vault-error/40",
};

export function StatusBadge({ status }: { status: DocStatus }) {
  return (
    <span
      className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${styles[status]}`}
    >
      <span
        className={`w-1.5 h-1.5 rounded-full ${
          status === "Ready"
            ? "bg-vault-text"
            : status === "Processing"
            ? "bg-vault-warning"
            : status === "Failed"
            ? "bg-vault-error"
            : "bg-vault-muted"
        }`}
      />
      {status}
    </span>
  );
}
