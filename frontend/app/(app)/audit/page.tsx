"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";

import { TopBar } from "@/components/TopBar";
import { SwissCard } from "@/components/ui/Card";

import {
  getAuditLogs,
  AuditLogResponse,
} from "@/lib/api";

import {
  ChevronDown,
  ArrowRight,
  RefreshCw,
  ChevronLeft,
  ChevronRight,
} from "lucide-react";

type ActionFilter =
  | "ALL"
  | "DOCUMENT_UPLOADED"
  | "DOCUMENT_ACCESSED"
  | "RAG_QUERY_EXECUTED";

type TimeFilter =
  | "24H"
  | "7D"
  | "30D"
  | "ALL";

export default function AuditPage() {
  const [logs, setLogs] =
    useState<AuditLogResponse[]>([]);

  const [selected, setSelected] =
    useState<AuditLogResponse | null>(null);

  const [loading, setLoading] =
    useState(true);

  const [refreshing, setRefreshing] =
    useState(false);

  const [error, setError] =
    useState<string | null>(null);

  const [actionFilter, setActionFilter] =
    useState<ActionFilter>("ALL");

  const [timeFilter, setTimeFilter] =
    useState<TimeFilter>("24H");

  const [page, setPage] =
    useState(0);

  const [totalPages, setTotalPages] =
    useState(0);

  const [totalElements, setTotalElements] =
    useState(0);

  const PAGE_SIZE = 20;

  const loadAuditLogs = async (
    pageNumber = page,
    showRefresh = false
  ) => {
    try {
      if (showRefresh) {
        setRefreshing(true);
      } else {
        setLoading(true);
      }

      setError(null);

      const response =
        await getAuditLogs(
          pageNumber,
          PAGE_SIZE
        );

      setLogs(response.content);
      setPage(response.page);
      setTotalPages(response.totalPages);
      setTotalElements(
        response.totalElements
      );

      /*
       * Keep the selected event if it still
       * exists in the refreshed result.
       */
      setSelected((current) => {
        if (!current) {
          return null;
        }

        return (
          response.content.find(
            (log) => log.id === current.id
          ) || null
        );
      });
    } catch (error) {
      
      if (
    error instanceof Error &&
    error.message === "You are not authenticated"
  ) {
    return;
  }
      
      console.error(
        "Failed to load audit logs:",
        error
      );

      setError(
        error instanceof Error
          ? error.message
          : "Failed to load audit logs."
      );
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    loadAuditLogs(0);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  /*
   * Client-side filtering of the currently
   * loaded page.
   */
  const filteredLogs = useMemo(() => {
    const now = Date.now();

    return logs.filter((log) => {
      /*
       * Action filter
       */
      if (
        actionFilter !== "ALL" &&
        log.action !== actionFilter
      ) {
        return false;
      }

      /*
       * Time filter
       */
      if (timeFilter === "ALL") {
        return true;
      }

      const createdAt =
        new Date(log.createdAt).getTime();

      if (Number.isNaN(createdAt)) {
        return false;
      }

      const age =
        now - createdAt;

      const oneDay =
        24 * 60 * 60 * 1000;

      if (
        timeFilter === "24H" &&
        age > oneDay
      ) {
        return false;
      }

      if (
        timeFilter === "7D" &&
        age > 7 * oneDay
      ) {
        return false;
      }

      if (
        timeFilter === "30D" &&
        age > 30 * oneDay
      ) {
        return false;
      }

      return true;
    });
  }, [
    logs,
    actionFilter,
    timeFilter,
  ]);

  const handleRefresh = async () => {
    await loadAuditLogs(
      page,
      true
    );
  };

  const handlePreviousPage = async () => {
    if (page <= 0) {
      return;
    }

    setSelected(null);

    await loadAuditLogs(
      page - 1
    );
  };

  const handleNextPage = async () => {
    if (
      page >= totalPages - 1
    ) {
      return;
    }

    setSelected(null);

    await loadAuditLogs(
      page + 1
    );
  };

  return (
    <>
      <TopBar
        title="Audit Logs"
        subtitle="Security-relevant activity across your workspace"
      />

      <div className="p-8 max-w-6xl">
        {/* Header controls */}
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-3">
            {/* Action filter */}
            <div className="relative">
              <select
                value={actionFilter}
                onChange={(event) => {
                  setActionFilter(
                    event.target
                      .value as ActionFilter
                  );
                }}
                className="appearance-none text-sm border border-vault-border/20 rounded-swiss px-3 pr-8 py-1.5 text-vault-muted bg-transparent outline-none cursor-pointer"
              >
                <option value="ALL">
                  All actions
                </option>

                <option value="DOCUMENT_UPLOADED">
                  Document uploaded
                </option>

                <option value="DOCUMENT_ACCESSED">
                  Document accessed
                </option>

                <option value="RAG_QUERY_EXECUTED">
                  RAG query
                </option>
              </select>

              <ChevronDown
                size={13}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-vault-muted"
              />
            </div>

            {/* Time filter */}
            <div className="relative">
              <select
                value={timeFilter}
                onChange={(event) => {
                  setTimeFilter(
                    event.target
                      .value as TimeFilter
                  );
                }}
                className="appearance-none text-sm border border-vault-border/20 rounded-swiss px-3 pr-8 py-1.5 text-vault-muted bg-transparent outline-none cursor-pointer"
              >
                <option value="24H">
                  Last 24 hours
                </option>

                <option value="7D">
                  Last 7 days
                </option>

                <option value="30D">
                  Last 30 days
                </option>

                <option value="ALL">
                  All time
                </option>
              </select>

              <ChevronDown
                size={13}
                className="absolute right-2.5 top-1/2 -translate-y-1/2 pointer-events-none text-vault-muted"
              />
            </div>

            {/* Refresh */}
            <button
              type="button"
              onClick={handleRefresh}
              disabled={refreshing}
              className="flex items-center gap-1.5 text-sm text-vault-muted hover:text-vault-text transition-colors disabled:opacity-50"
              title="Refresh audit logs"
            >
              <RefreshCw
                size={14}
                className={
                  refreshing
                    ? "animate-spin"
                    : ""
                }
              />

              Refresh
            </button>
          </div>

          <Link
            href="/ask"
            className="hidden sm:inline-flex items-center gap-1 text-sm text-vault-muted hover:text-vault-text"
          >
            Back to Ask
            <ArrowRight size={13} />
          </Link>
        </div>

        {/* Error */}
        {error && (
          <SwissCard className="p-4 mb-4">
            <div className="flex items-center justify-between gap-4">
              <p className="text-sm text-red-600">
                {error}
              </p>

              <button
                type="button"
                onClick={() =>
                  loadAuditLogs(
                    page,
                    true
                  )
                }
                className="text-sm underline text-vault-muted hover:text-vault-text"
              >
                Retry
              </button>
            </div>
          </SwissCard>
        )}

        <div className="grid grid-cols-5 gap-6">
          {/* Audit table */}
          <SwissCard className="col-span-3 p-0 overflow-hidden">
            <div className="grid grid-cols-[80px_110px_1fr] px-5 py-3 text-xs text-vault-muted border-b border-vault-border/10 font-mono">
              <span>TIME</span>
              <span>USER</span>
              <span>ACTION</span>
            </div>

            {loading ? (
              <div className="px-5 py-10 text-center">
                <p className="text-sm text-vault-muted">
                  Loading audit logs...
                </p>
              </div>
            ) : filteredLogs.length === 0 ? (
              <div className="px-5 py-10 text-center">
                <p className="text-sm font-medium">
                  No audit events found
                </p>

                <p className="text-xs text-vault-muted mt-1">
                  Try changing the filters or
                  perform an action in VaultRAG.
                </p>
              </div>
            ) : (
              filteredLogs.map((log) => (
                <button
                  key={log.id}
                  type="button"
                  onClick={() =>
                    setSelected(log)
                  }
                  className={`grid grid-cols-[80px_110px_1fr] w-full text-left px-5 py-3 text-sm border-b border-vault-border/5 last:border-0 transition-colors ${
                    selected?.id === log.id
                      ? "bg-vault-secondary"
                      : "hover:bg-vault-secondary/60"
                  }`}
                >
                  <span className="font-mono text-xs text-vault-muted">
                    {formatTime(
                      log.createdAt
                    )}
                  </span>

                  <span className="truncate">
                    {log.username ||
                      "Unknown"}
                  </span>

                  <span className="text-vault-text/80 truncate">
                    {formatAction(
                      log.action
                    )}
                  </span>
                </button>
              ))
            )}

            {/* Pagination */}
            {!loading &&
              filteredLogs.length > 0 && (
                <div className="flex items-center justify-between px-5 py-3 border-t border-vault-border/10">
                  <p className="text-xs text-vault-muted">
                    {totalElements}{" "}
                    {totalElements === 1
                      ? "event"
                      : "events"}
                  </p>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={
                        handlePreviousPage
                      }
                      disabled={
                        page === 0 ||
                        refreshing
                      }
                      className="p-1.5 rounded border border-vault-border/20 text-vault-muted hover:text-vault-text disabled:opacity-30 disabled:cursor-not-allowed"
                      title="Previous page"
                    >
                      <ChevronLeft
                        size={14}
                      />
                    </button>

                    <span className="text-xs text-vault-muted font-mono">
                      {page + 1} /{" "}
                      {Math.max(
                        totalPages,
                        1
                      )}
                    </span>

                    <button
                      type="button"
                      onClick={
                        handleNextPage
                      }
                      disabled={
                        page >=
                          totalPages - 1 ||
                        refreshing
                      }
                      className="p-1.5 rounded border border-vault-border/20 text-vault-muted hover:text-vault-text disabled:opacity-30 disabled:cursor-not-allowed"
                      title="Next page"
                    >
                      <ChevronRight
                        size={14}
                      />
                    </button>
                  </div>
                </div>
              )}
          </SwissCard>

          {/* Event detail */}
          <SwissCard className="col-span-2 p-5 h-fit">
            <p className="text-sm font-medium mb-4">
              Event detail
            </p>

            {selected ? (
              <dl className="space-y-3 text-sm">
                <Row
                  label="Action"
                  value={selected.action}
                  mono
                />

                <Row
                  label="User"
                  value={
                    selected.username ||
                    "Unknown"
                  }
                />

                <Row
                  label="Resource"
                  value={formatResource(
                    selected
                  )}
                />

                <Row
                  label="Time"
                  value={formatDateTime(
                    selected.createdAt
                  )}
                />

                <Row
                  label="Event ID"
                  value={selected.id}
                  mono
                />
              </dl>
            ) : (
              <p className="text-sm text-vault-muted">
                Select an event to see its
                details.
              </p>
            )}

            <p className="text-xs text-vault-muted mt-5 pt-4 border-t border-vault-border/10">
              Document content is never shown
              in audit records — only the action
              taken and the resource it touched.
            </p>
          </SwissCard>
        </div>
      </div>
    </>
  );
}

function Row({
  label,
  value,
  mono,
}: {
  label: string;
  value: string;
  mono?: boolean;
}) {
  return (
    <div>
      <dt className="text-xs text-vault-muted">
        {label}
      </dt>

      <dd
        className={
          mono
            ? "font-mono text-xs mt-0.5 break-all"
            : "mt-0.5 break-words"
        }
      >
        {value}
      </dd>
    </div>
  );
}

function formatTime(
  value: string
): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "--:--";
  }

  return date.toLocaleTimeString(
    "en-US",
    {
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }
  );
}

function formatDateTime(
  value: string
): string {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return "Unknown";
  }

  const today =
    new Date();

  const isToday =
    date.toDateString() ===
    today.toDateString();

  if (isToday) {
    return `Today, ${date.toLocaleTimeString(
      "en-US",
      {
        hour: "2-digit",
        minute: "2-digit",
        hour12: false,
      }
    )}`;
  }

  return date.toLocaleString(
    "en-US",
    {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
      hour12: false,
    }
  );
}

function formatAction(
  action: string
): string {
  return action
    .replaceAll("_", " ");
}

function formatResource(
  log: AuditLogResponse
): string {
  if (!log.resourceId) {
    return log.resourceType || "—";
  }

  if (!log.resourceType) {
    return log.resourceId;
  }

  return `${log.resourceType} · ${log.resourceId}`;
}