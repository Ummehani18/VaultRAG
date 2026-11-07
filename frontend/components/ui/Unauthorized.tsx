"use client";

import Link from "next/link";
import { ShieldAlert, LogIn } from "lucide-react";
import { Button } from "@/components/ui/Button";

type Reason = "expired" | "signed-out";

const copy: Record<Reason, { title: string; body: string }> = {
  expired: {
    title: "Your session has expired.",
    body: "For your workspace's security, you've been signed out after a period of inactivity. Sign in again to pick up where you left off.",
  },
  "signed-out": {
    title: "Sign in to continue.",
    body: "This page is part of your organization's secure document workspace. You'll need to sign in before VaultRAG can show it to you.",
  },
};

export function Unauthorized({
  reason = "signed-out",
  returnTo,
}: {
  reason?: Reason;
  returnTo?: string;
}) {
  const { title, body } = copy[reason];

  const safeReturnTo =
    returnTo &&
    returnTo.startsWith("/") &&
    !returnTo.startsWith("//") &&
    !returnTo.startsWith("/login") &&
    !returnTo.startsWith("/unauthorized")
      ? returnTo
      : undefined;

  const loginHref = safeReturnTo
    ? `/login?returnTo=${encodeURIComponent(safeReturnTo)}`
    : "/login";

  return (
    <div
      className="fixed inset-0 z-[100] flex min-h-screen items-center justify-center bg-black/35 p-6 backdrop-blur-md"
      role="dialog"
      aria-modal="true"
      aria-labelledby="unauthorized-title"
      aria-describedby="unauthorized-description"
    >
      <div className="w-full max-w-md rounded-swiss border border-vault-border/20 bg-vault-surface p-8 text-center shadow-2xl">
        <div className="mx-auto mb-6 flex h-14 w-14 items-center justify-center rounded-full border-2 border-vault-border bg-vault-warning-bg">
          <ShieldAlert
            size={24}
            strokeWidth={2}
            className="text-vault-warning"
          />
        </div>

        <p className="mb-3 font-mono text-xs tracking-wide text-vault-muted">
          ERROR 401 · UNAUTHORIZED
        </p>

        <h1
          id="unauthorized-title"
          className="mb-3 font-serif text-3xl leading-snug"
        >
          {title}
        </h1>

        <p
          id="unauthorized-description"
          className="mx-auto mb-8 max-w-sm text-sm text-vault-muted"
        >
          {body}
        </p>

        <div className="flex flex-wrap items-center justify-center gap-3">
          <Link href={loginHref}>
            <Button variant="brutal" icon={<LogIn size={15} />}>
              Sign in
            </Button>
          </Link>

          <Link href="/">
            <Button variant="swiss">
              Go to homepage
            </Button>
          </Link>
        </div>

        <p className="mt-6 text-xs text-vault-muted">
          Repeated sign-in failures are recorded in the security audit log.
        </p>
      </div>
    </div>
  );
}
