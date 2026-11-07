"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { LogOut, Search } from "lucide-react";

interface StoredUser {
  id: string;
  username: string;
  role: string;
}

export function TopBar({
  title,
  subtitle,
}: {
  title: string;
  subtitle?: string;
}) {
  const router = useRouter();

  const [user, setUser] = useState<StoredUser | null>(null);
  const [menuOpen, setMenuOpen] = useState(false);

  useEffect(() => {
    const storedUser = sessionStorage.getItem("vaultrag_user");

    if (storedUser) {
      try {
        setUser(JSON.parse(storedUser));
      } catch {
        sessionStorage.removeItem("vaultrag_user");
      }
    }
  }, []);

  function getInitials(username: string) {
    return username
      .trim()
      .split(/\s+/)
      .map((part) => part[0])
      .join("")
      .slice(0, 2)
      .toUpperCase();
  }

  function handleLogout() {
    sessionStorage.removeItem("vaultrag_token");
    sessionStorage.removeItem("vaultrag_user");

    router.push("/login");
  }

  const username = user?.username || "User";
  const initials = getInitials(username);

  return (
    <header className="h-16 border-b border-vault-border/15 flex items-center justify-between px-8">
      <div>
        <h1 className="text-[15px] font-medium">{title}</h1>

        {subtitle && (
          <p className="text-xs text-vault-muted mt-0.5">
            {subtitle}
          </p>
        )}
      </div>

      <div className="flex items-center gap-4">
        

        <div className="relative">
          <button
            type="button"
            onClick={() => setMenuOpen((current) => !current)}
            aria-label="Open profile menu"
            aria-expanded={menuOpen}
            className="w-8 h-8 rounded-full bg-vault-text text-vault-bg text-xs font-medium flex items-center justify-center hover:opacity-80 transition-opacity"
          >
            {initials}
          </button>

          {menuOpen && (
            <div className="absolute right-0 top-11 w-48 bg-vault-surface border border-vault-border/20 rounded-swiss shadow-sm z-50">
              <div className="px-3 py-2.5 border-b border-vault-border/10">
                <p className="text-xs text-vault-muted">
                  Signed in as
                </p>

                <p className="text-sm font-medium truncate mt-0.5">
                  {username}
                </p>
              </div>

              <button
                type="button"
                onClick={handleLogout}
                className="w-full flex items-center gap-2 px-3 py-2.5 text-sm text-left hover:bg-vault-secondary transition-colors"
              >
                <LogOut size={15} />
                <span>Log out</span>
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}