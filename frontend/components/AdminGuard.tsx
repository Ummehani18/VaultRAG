"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";

interface AdminGuardProps {
  children: React.ReactNode;
}

export function AdminGuard({
  children,
}: AdminGuardProps) {
  const router = useRouter();

  const [checking, setChecking] = useState(true);
  const [authorized, setAuthorized] = useState(false);

  useEffect(() => {
    const token =
      sessionStorage.getItem("vaultrag_token");

    const storedUser =
      sessionStorage.getItem("vaultrag_user");

    if (!token) {
      router.replace("/login");
      return;
    }

    if (!storedUser) {
      router.replace("/ask");
      return;
    }

    try {
      const user = JSON.parse(storedUser);

      if (user.role?.toUpperCase() !== "ADMIN") {
        router.replace("/ask");
        return;
      }

      setAuthorized(true);
    } catch {
      router.replace("/ask");
    } finally {
      setChecking(false);
    }
  }, [router]);

  if (checking) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-vault-bg text-vault-text">
        <p className="text-sm text-vault-muted">
          Checking administrator access...
        </p>
      </div>
    );
  }

  if (!authorized) {
    return null;
  }

  return <>{children}</>;
}