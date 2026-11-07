"use client";

import { useEffect, useState } from "react";
import { useSessionOverlay } from "@/components/SessionOverlayProvider";

interface AuthGuardProps {
  children: React.ReactNode;
}

export function AuthGuard({ children }: AuthGuardProps) {
  const [checking, setChecking] = useState(true);
  const { showUnauthorized } = useSessionOverlay();

  useEffect(() => {
    const token = sessionStorage.getItem("vaultrag_token");

    if (!token) {
      showUnauthorized();
    }

    setChecking(false);
  }, [showUnauthorized]);

  if (checking) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-vault-bg text-vault-text">
        <p className="text-sm text-vault-muted">
          Checking authentication...
        </p>
      </div>
    );
  }

  return <>{children}</>;
}