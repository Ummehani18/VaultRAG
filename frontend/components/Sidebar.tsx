"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { usePathname } from "next/navigation";
import {
  FileStack,
  MessagesSquare,
  ShieldCheck,
  Lock,
  LayoutDashboard,
} from "lucide-react";

const primary = {
  href: "/ask",
  label: "Ask VaultRAG",
  icon: MessagesSquare,
};

const supporting = [
  {
    href: "/documents",
    label: "Documents",
    icon: FileStack,
  },
  {
    href: "/audit",
    label: "Audit Logs",
    icon: ShieldCheck,
  },
];

const adminLinks = [
  {
    href: "/admin",
    label: "Admin Dashboard",
    icon: LayoutDashboard,
  },
];

export function Sidebar() {
  const pathname = usePathname();

  const [role, setRole] = useState<string | null>(null);

  useEffect(() => {
    const storedUser =
      sessionStorage.getItem("vaultrag_user");

    if (!storedUser) {
      return;
    }

    try {
      const user = JSON.parse(storedUser);

      setRole(user.role ?? null);
    } catch {
      setRole(null);
    }
  }, []);

  const isAdmin = role?.toUpperCase() === "ADMIN";

  const askActive = pathname === "/ask";

  return (
    <aside className="w-60 shrink-0 border-r border-vault-border/15 bg-vault-bg flex flex-col justify-between min-h-screen">
      <div>
        <div className="px-5 pt-5 pb-4 border-b border-vault-border/15">
          <div className="flex items-center gap-2 mb-1.5">
            <Lock size={16} strokeWidth={2.5} />

            <span className="font-medium tracking-tight text-[15px]">
              VaultRAG
            </span>
          </div>

          <p className="text-xs text-vault-muted leading-snug">
            Private document intelligence workspace
          </p>
        </div>

        {/* Primary workflow */}
        <div className="px-3 pt-4">
          <Link
            href={primary.href}
            className={`flex items-center gap-2.5 px-3 py-2.5 rounded-brutal border-2 border-vault-border text-sm font-medium transition-transform ${
              askActive
                ? "bg-vault-accent shadow-none translate-x-[2px] translate-y-[2px]"
                : "bg-vault-accent shadow-brutal-sm hover:-translate-y-[1px] hover:shadow-brutal"
            }`}
          >
            <primary.icon size={16} strokeWidth={2.25} />
            {primary.label}
          </Link>
        </div>

        {/* Supporting areas */}
        <nav className="px-3 pt-6">
          <p className="px-3 mb-1.5 text-[11px] font-mono tracking-wide text-vault-muted">
            WORKSPACE
          </p>

          <div className="flex flex-col gap-0.5">
            {supporting.map((link) => {
              const active = pathname === link.href;
              const Icon = link.icon;

              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`flex items-center gap-2.5 px-3 py-2 rounded-swiss text-sm transition-colors ${
                    active
                      ? "bg-vault-secondary text-vault-text"
                      : "text-vault-text/65 hover:bg-vault-secondary/60"
                  }`}
                >
                  <Icon size={16} strokeWidth={2} />
                  {link.label}
                </Link>
              );
            })}

            {/* ADMIN AREA */}
            {isAdmin && (
              <>
                <p className="px-3 mt-5 mb-1.5 text-[11px] font-mono tracking-wide text-vault-muted">
                  ADMINISTRATION
                </p>

                {adminLinks.map((link) => {
                  const active = pathname === link.href;
                  const Icon = link.icon;

                  return (
                    <Link
                      key={link.href}
                      href={link.href}
                      className={`flex items-center gap-2.5 px-3 py-2 rounded-swiss text-sm transition-colors ${
                        active
                          ? "bg-vault-secondary text-vault-text"
                          : "text-vault-text/65 hover:bg-vault-secondary/60"
                      }`}
                    >
                      <Icon size={16} strokeWidth={2} />
                      {link.label}
                    </Link>
                  );
                })}
              </>
            )}
          </div>
        </nav>
      </div>

      <div className="px-5 py-5 border-t border-vault-border/15">
        <p className="text-xs text-vault-muted leading-relaxed">
          Documents never leave your organization&apos;s environment.
          Retrieval and generation run inside your secure workspace.
        </p>
      </div>
    </aside>
  );
}