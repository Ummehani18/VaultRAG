
"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
} from "react";
import { usePathname } from "next/navigation";
import { Unauthorized } from "@/components/ui/Unauthorized";

type SessionOverlayContextValue = {
  showUnauthorized: () => void;
};

const SessionOverlayContext =
  createContext<SessionOverlayContextValue | null>(null);

export function useSessionOverlay() {
  const context = useContext(SessionOverlayContext);

  if (!context) {
    throw new Error(
      "useSessionOverlay must be used inside SessionOverlayProvider"
    );
  }

  return context;
}

export function SessionOverlayProvider({
  children,
}: {
  children: React.ReactNode;
}) {
  const [showOverlay, setShowOverlay] = useState(false);
  const [returnTo, setReturnTo] = useState("/ask");

  const pathname = usePathname();


  const showUnauthorized = useCallback(() => {
    if (window.location.pathname !== "/login") {
      setReturnTo(
        window.location.pathname + window.location.search
      );
    }

    sessionStorage.removeItem("vaultrag_token");
    sessionStorage.removeItem("vaultrag_user");
    setShowOverlay(true);
  }, []);

  useEffect(() => {
    const handleSessionExpired = () => {
      showUnauthorized();
    };

    window.addEventListener(
      "vaultrag:unauthorized",
      handleSessionExpired
    );

    return () => {
      window.removeEventListener(
        "vaultrag:unauthorized",
        handleSessionExpired
      );
    };
  }, [showUnauthorized]);

  useEffect(() => {
    if (pathname === "/login") {
      setShowOverlay(false);
    }
  }, [pathname]);

  return (
    <SessionOverlayContext.Provider value={{ showUnauthorized }}>
      {children}

      {showOverlay && pathname !== "/login" && (
        <Unauthorized
          reason="expired"
          returnTo={returnTo}
        />
      )}
    </SessionOverlayContext.Provider>
  );
}
