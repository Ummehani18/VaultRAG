"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import { TopBar } from "@/components/TopBar";
import { SwissCard, BrutalCard } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { AdminGuard } from "@/components/AdminGuard";
import {
  getAdminDashboard,
  getAdminUsers,
  getUserAuditLogs,
  AdminDashboardResponse,
  AdminUserSummary,
  AuditLogEntry,
} from "@/lib/api";
import {
  Users,
  ShieldCheck,
  FileStack,
  ScrollText,
  TriangleAlert,
  RotateCcw,
  Search,
  X,
  ScrollText as LogIcon,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

export default function AdminDashboardPage() {
  return (
    <AdminGuard>
      <AdminDashboardContent />
    </AdminGuard>
  );
}

function AdminDashboardContent() {
  // =========================================================
  // SUMMARY STATS
  // =========================================================

  const [dashboard, setDashboard] =
    useState<AdminDashboardResponse | null>(null);

  const [statsLoading, setStatsLoading] = useState(true);
  const [statsError, setStatsError] = useState<string | null>(null);

  // =========================================================
  // USER LIST + PAGINATION
  // =========================================================

  const [users, setUsers] = useState<AdminUserSummary[]>([]);
  const [usersLoading, setUsersLoading] = useState(true);
  const [usersError, setUsersError] = useState<string | null>(null);

  const [search, setSearch] = useState("");

  const [userPage, setUserPage] = useState(0);
  const [userPageSize] = useState(20);

  const [userTotalElements, setUserTotalElements] = useState(0);
  const [userTotalPages, setUserTotalPages] = useState(0);
  const [userFirst, setUserFirst] = useState(true);
  const [userLast, setUserLast] = useState(true);

  // =========================================================
  // SELECTED USER + AUDIT LOGS
  // =========================================================

  const [selectedUser, setSelectedUser] =
    useState<AdminUserSummary | null>(null);

  const [logs, setLogs] = useState<AuditLogEntry[] | null>(null);
  const [logsLoading, setLogsLoading] = useState(false);
  const [logsError, setLogsError] = useState<string | null>(null);

  // =========================================================
  // LOAD DASHBOARD STATS
  // =========================================================

  const loadStats = useCallback(async () => {
    try {
      setStatsLoading(true);
      setStatsError(null);

      const data = await getAdminDashboard();
      setDashboard(data);
    } catch (err) {
      setStatsError(
        err instanceof Error
          ? err.message
          : "Failed to load dashboard"
      );
    } finally {
      setStatsLoading(false);
    }
  }, []);

  // =========================================================
  // LOAD USERS
  // =========================================================

  const loadUsers = useCallback(async (page: number) => {
    try {
      setUsersLoading(true);
      setUsersError(null);

      const data = await getAdminUsers(
        page,
        userPageSize
      );

      setUsers(data.content);

      setUserPage(data.page);
      setUserTotalElements(data.totalElements);
      setUserTotalPages(data.totalPages);
      setUserFirst(data.first);
      setUserLast(data.last);
    } catch (err) {
      setUsersError(
        err instanceof Error
          ? err.message
          : "Failed to load users"
      );
    } finally {
      setUsersLoading(false);
    }
  }, [userPageSize]);

  // =========================================================
  // INITIAL LOAD
  // =========================================================

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadUsers(userPage);
  }, [loadUsers, userPage]);

  // =========================================================
  // SELECT USER
  // =========================================================

  async function selectUser(user: AdminUserSummary) {
    setSelectedUser(user);
    setLogs(null);
    setLogsError(null);
    setLogsLoading(true);

    try {
      const data = await getUserAuditLogs(user.id);
      setLogs(data);
    } catch (err) {
      setLogsError(
        err instanceof Error
          ? err.message
          : "Failed to load audit logs"
      );
    } finally {
      setLogsLoading(false);
    }
  }

  // =========================================================
  // CLEAR USER SELECTION
  // =========================================================

  function clearSelection() {
    setSelectedUser(null);
    setLogs(null);
    setLogsError(null);
  }

  // =========================================================
  // FILTER CURRENT PAGE
  // =========================================================

  const filteredUsers = useMemo(() => {
    const q = search.trim().toLowerCase();

    if (!q) {
      return users;
    }

    return users.filter((user) =>
      user.username.toLowerCase().includes(q)
    );
  }, [users, search]);

  // =========================================================
  // PAGINATION
  // =========================================================

  function goToPreviousPage() {
    if (userFirst || usersLoading) {
      return;
    }

    setUserPage((current) => Math.max(0, current - 1));
  }

  function goToNextPage() {
    if (userLast || usersLoading) {
      return;
    }

    setUserPage((current) => current + 1);
  }

  // =========================================================
  // PAGE
  // =========================================================

  return (
    <>
      <TopBar
        title="Admin Dashboard"
        subtitle="Users, documents, and audit activity across your workspace"
      />

      <div className="p-8 max-w-6xl space-y-6">

        {/* =====================================================
            STAT CARDS
        ====================================================== */}

        {statsLoading && <StatsSkeleton />}

        {!statsLoading && statsError && (
          <BrutalCard
            tone="error"
            className="p-6 flex items-start justify-between gap-6"
          >
            <div className="flex items-start gap-3">
              <TriangleAlert
                size={20}
                className="mt-0.5 shrink-0"
              />

              <div>
                <p className="text-sm font-medium">
                  Unable to load dashboard
                </p>

                <p className="text-sm text-vault-text/70 mt-1">
                  {statsError}
                </p>
              </div>
            </div>

            <Button
              variant="swiss"
              icon={<RotateCcw size={14} />}
              onClick={loadStats}
            >
              Retry
            </Button>
          </BrutalCard>
        )}

        {!statsLoading && !statsError && dashboard && (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
            <StatCard
              icon={Users}
              label="Total users"
              value={dashboard.totalUsers}
            />

            <StatCard
              icon={ShieldCheck}
              label="Administrators"
              value={dashboard.totalAdmins}
            />

            <StatCard
              icon={FileStack}
              label="Documents"
              value={dashboard.totalDocuments}
            />

            <StatCard
              icon={ScrollText}
              label="Audit events"
              value={dashboard.totalAuditLogs}
            />
          </div>
        )}

        {/* =====================================================
            USER FILTER + AUDIT LOG
        ====================================================== */}

        <div className="grid gap-6 lg:grid-cols-5">

          {/* ===================================================
              USER FILTER
          ==================================================== */}

          <SwissCard
            className="
              lg:col-span-2
              p-0
              overflow-hidden
              flex
              flex-col
              h-[520px]
            "
          >
            {/* Fixed search section */}
            <div className="p-4 border-b border-vault-border/10 shrink-0">
              <p className="text-sm font-medium mb-3">
                Filter by user
              </p>

              <div className="flex items-center gap-2 border border-vault-border/20 rounded-swiss px-3 py-2">
                <Search
                  size={14}
                  className="text-vault-muted shrink-0"
                />

                <input
                  value={search}
                  onChange={(event) =>
                    setSearch(event.target.value)
                  }
                  placeholder="Search username…"
                  className="
                    flex-1
                    bg-transparent
                    text-sm
                    outline-none
                    placeholder:text-vault-muted
                    min-w-0
                  "
                />

                {search && (
                  <button
                    type="button"
                    onClick={() => setSearch("")}
                    aria-label="Clear search"
                    className="shrink-0"
                  >
                    <X
                      size={13}
                      className="text-vault-muted hover:text-vault-text"
                    />
                  </button>
                )}
              </div>

              {/* User count */}
              {!usersLoading && !usersError && (
                <p className="text-[11px] text-vault-muted mt-2">
                  {userTotalElements.toLocaleString()} users
                </p>
              )}
            </div>

            {/* Scrollable user list */}
            <div className="flex-1 min-h-0 overflow-y-auto">
              {usersLoading && (
                <div className="p-4 space-y-3 animate-pulse">
                  {Array.from({ length: 8 }).map(
                    (_, index) => (
                      <div
                        key={index}
                        className="h-10 bg-vault-secondary rounded-swiss"
                      />
                    )
                  )}
                </div>
              )}

              {!usersLoading && usersError && (
                <div className="p-4">
                  <p className="text-sm text-vault-error mb-2">
                    {usersError}
                  </p>

                  <Button
                    variant="swiss"
                    icon={<RotateCcw size={13} />}
                    onClick={() => loadUsers(userPage)}
                  >
                    Retry
                  </Button>
                </div>
              )}

              {!usersLoading &&
                !usersError &&
                filteredUsers.length === 0 && (
                  <p className="p-5 text-sm text-vault-muted">
                    No users match &ldquo;{search}&rdquo;.
                  </p>
                )}

              {!usersLoading &&
                !usersError &&
                filteredUsers.map((user) => {
                  const active =
                    selectedUser?.id === user.id;

                  return (
                    <button
                      key={user.id}
                      type="button"
                      onClick={() => selectUser(user)}
                      className={`
                        w-full
                        flex
                        items-center
                        justify-between
                        gap-3
                        px-4
                        py-3
                        border-b
                        border-vault-border/5
                        last:border-0
                        text-left
                        transition-colors
                        ${
                          active
                            ? "bg-vault-secondary"
                            : "hover:bg-vault-secondary/50"
                        }
                      `}
                    >
                      <div className="min-w-0">
                        <p className="text-sm font-medium truncate">
                          {user.username}
                        </p>

                        <p className="text-xs text-vault-muted truncate">
                          {user.createdAt
                            ? new Date(
                                user.createdAt
                              ).toLocaleDateString()
                            : ""}
                        </p>
                      </div>

                      <span
                        className={`
                          shrink-0
                          text-[10px]
                          font-mono
                          px-2
                          py-1
                          rounded-full
                          ${
                            user.role === "ADMIN"
                              ? "bg-vault-text text-vault-bg"
                              : "bg-vault-secondary text-vault-muted"
                          }
                        `}
                      >
                        {user.role}
                      </span>
                    </button>
                  );
                })}
            </div>

            {/* User pagination controls */}
            {!usersLoading &&
              !usersError &&
              userTotalPages > 0 && (
                <div
                  className="
                    shrink-0
                    flex
                    items-center
                    justify-between
                    gap-3
                    px-4
                    py-3
                    border-t
                    border-vault-border/10
                  "
                >
                  <button
                    type="button"
                    onClick={goToPreviousPage}
                    disabled={userFirst}
                    className="
                      inline-flex
                      items-center
                      gap-1.5
                      text-xs
                      text-vault-muted
                      hover:text-vault-text
                      disabled:opacity-30
                      disabled:cursor-not-allowed
                    "
                  >
                    <ChevronLeft size={14} />
                    Previous
                  </button>

                  <span className="text-[11px] text-vault-muted font-mono">
                    Page {userPage + 1} of {userTotalPages}
                  </span>

                  <button
                    type="button"
                    onClick={goToNextPage}
                    disabled={userLast}
                    className="
                      inline-flex
                      items-center
                      gap-1.5
                      text-xs
                      text-vault-muted
                      hover:text-vault-text
                      disabled:opacity-30
                      disabled:cursor-not-allowed
                    "
                  >
                    Next
                    <ChevronRight size={14} />
                  </button>
                </div>
              )}
          </SwissCard>

          {/* ===================================================
              AUDIT LOG
          ==================================================== */}

          <SwissCard
            className="
              lg:col-span-3
              p-0
              overflow-hidden
              flex
              flex-col
              h-[520px]
            "
          >
            {/* Fixed audit header */}
            <div
              className="
                flex
                items-center
                justify-between
                px-5
                py-4
                border-b
                border-vault-border/10
                shrink-0
              "
            >
              <div className="min-w-0">
                <p className="text-sm font-medium">
                  {selectedUser
                    ? `Audit log — ${selectedUser.username}`
                    : "Audit log"}
                </p>

                {selectedUser && (
                  <p className="text-xs text-vault-muted truncate">
                    {selectedUser.username}
                  </p>
                )}
              </div>

              {selectedUser && (
                <button
                  type="button"
                  onClick={clearSelection}
                  className="
                    text-xs
                    text-vault-muted
                    hover:text-vault-text
                    shrink-0
                  "
                >
                  Clear
                </button>
              )}
            </div>

            {/* Scrollable audit content */}
            <div className="flex-1 min-h-0 overflow-y-auto">
              {!selectedUser && (
                <div className="min-h-full flex flex-col items-center justify-center text-center p-10 gap-3">
                  <div className="w-10 h-10 rounded-full bg-vault-secondary flex items-center justify-center">
                    <LogIcon
                      size={17}
                      className="text-vault-muted"
                    />
                  </div>

                  <p className="text-sm text-vault-muted max-w-xs">
                    Select a user on the left to see every
                    security-relevant action they&apos;ve
                    taken in this workspace.
                  </p>
                </div>
              )}

              {selectedUser && logsLoading && (
                <div className="p-5 space-y-3 animate-pulse">
                  {Array.from({ length: 8 }).map(
                    (_, index) => (
                      <div
                        key={index}
                        className="h-3 bg-vault-secondary rounded w-full"
                      />
                    )
                  )}
                </div>
              )}

              {selectedUser &&
                !logsLoading &&
                logsError && (
                  <div className="p-5">
                    <p className="text-sm text-vault-error mb-2">
                      {logsError}
                    </p>

                    <Button
                      variant="swiss"
                      icon={<RotateCcw size={13} />}
                      onClick={() =>
                        selectUser(selectedUser)
                      }
                    >
                      Retry
                    </Button>
                  </div>
                )}

              {selectedUser &&
                !logsLoading &&
                !logsError &&
                logs &&
                logs.length === 0 && (
                  <p className="p-5 text-sm text-vault-muted">
                    No audit events recorded for this user yet.
                  </p>
                )}

              {selectedUser &&
                !logsLoading &&
                !logsError &&
                logs &&
                logs.length > 0 && (
                  <div>
                    {/* Sticky table header */}
                    <div
                      className="
                        grid
                        grid-cols-[80px_1fr_110px]
                        px-5
                        py-2.5
                        text-xs
                        text-vault-muted
                        border-b
                        border-vault-border/10
                        font-mono
                        sticky
                        top-0
                        bg-vault-surface
                        z-10
                      "
                    >
                      <span>TIME</span>
                      <span>ACTION</span>
                      <span>RESOURCE</span>
                    </div>

                    {/* Audit rows */}
                    {logs.map((log) => (
                      <div
                        key={log.id}
                        className="
                          grid
                          grid-cols-[80px_1fr_110px]
                          px-5
                          py-3
                          text-sm
                          border-b
                          border-vault-border/5
                          last:border-0
                        "
                      >
                        <span className="font-mono text-xs text-vault-muted">
                          {log.time}
                        </span>

                        <span className="truncate">
                          {log.action
                            .replaceAll("_", " ")
                            .toLowerCase()}
                        </span>

                        <span className="text-xs text-vault-muted truncate">
                          {log.resource}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
            </div>
          </SwissCard>
        </div>
      </div>
    </>
  );
}

// =============================================================
// STAT CARD
// =============================================================

function StatCard({
  icon: Icon,
  label,
  value,
}: {
  icon: typeof Users;
  label: string;
  value: number;
}) {
  return (
    <SwissCard className="p-5">
      <div className="flex items-center justify-between mb-3">
        <p className="text-xs text-vault-muted">
          {label}
        </p>

        <div className="w-7 h-7 rounded-full bg-vault-secondary flex items-center justify-center">
          <Icon size={14} />
        </div>
      </div>

      <p className="text-3xl font-medium">
        {value.toLocaleString()}
      </p>
    </SwissCard>
  );
}

// =============================================================
// STATS SKELETON
// =============================================================

function StatsSkeleton() {
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4 animate-pulse">
      {Array.from({ length: 4 }).map((_, index) => (
        <SwissCard
          key={index}
          className="p-5"
        >
          <div className="h-3 w-20 bg-vault-secondary rounded mb-4" />
          <div className="h-7 w-16 bg-vault-secondary rounded" />
        </SwissCard>
      ))}
    </div>
  );
}