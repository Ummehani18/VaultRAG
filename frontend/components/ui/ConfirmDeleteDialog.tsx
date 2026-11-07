"use client";

import { useEffect, useRef } from "react";
import {
  TriangleAlert,
  Trash2,
  X,
  Loader2,
} from "lucide-react";

type Props = {
  open: boolean;
  documentName: string;
  loading?: boolean;
  onCancel: () => void;
  onConfirm: () => void;
};

export function ConfirmDeleteDialog({
  open,
  documentName,
  loading = false,
  onCancel,
  onConfirm,
}: Props) {
  const cancelRef =
    useRef<HTMLButtonElement>(null);

  useEffect(() => {
    if (!open) return;

    const onKey = (e: KeyboardEvent) => {
      if (
        e.key === "Escape" &&
        !loading
      ) {
        onCancel();
      }
    };

    document.addEventListener(
      "keydown",
      onKey
    );

    const prevOverflow =
      document.body.style.overflow;

    document.body.style.overflow =
      "hidden";

    /*
     * Focus the Cancel button when
     * the dialog opens.
     */
    cancelRef.current?.focus();

    return () => {
      document.removeEventListener(
        "keydown",
        onKey
      );

      document.body.style.overflow =
        prevOverflow;
    };
  }, [
    open,
    loading,
    onCancel,
  ]);

  if (!open) {
    return null;
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-vault-text/40"
      onMouseDown={(e) => {
        if (
          e.target === e.currentTarget &&
          !loading
        ) {
          onCancel();
        }
      }}
    >
      <div
        role="alertdialog"
        aria-modal="true"
        aria-labelledby="delete-dialog-title"
        aria-describedby="delete-dialog-desc"
        className="relative w-full max-w-md bg-vault-surface border-2 border-vault-border rounded-brutal shadow-brutal p-6"
      >
        {/* Close button */}

        <button
          type="button"
          onClick={onCancel}
          disabled={loading}
          aria-label="Close"
          className="absolute top-4 right-4 text-vault-muted hover:text-vault-text disabled:opacity-40"
        >
          <X size={16} />
        </button>

        {/* Warning icon */}

        <div className="w-10 h-10 rounded-full bg-vault-error-bg border-2 border-vault-border flex items-center justify-center mb-4">
          <TriangleAlert
            size={18}
            className="text-vault-error"
          />
        </div>

        {/* Title */}

        <h2
          id="delete-dialog-title"
          className="font-serif text-2xl leading-snug mb-2"
        >
          Delete this document?
        </h2>

        {/* Description */}

        <p
          id="delete-dialog-desc"
          className="text-sm text-vault-muted mb-4"
        >
          This permanently removes the file
          and its indexed content from your
          vault. VaultRAG will no longer use it
          to answer questions or cite it as a
          source.
        </p>

        {/* Document */}

        <div className="border border-vault-border/15 bg-vault-bg rounded-swiss px-3 py-2.5 mb-6">
          <p className="text-xs text-vault-muted mb-0.5">
            Document
          </p>

          <p className="text-sm font-medium truncate">
            {documentName}
          </p>
        </div>

        {/* Actions */}

        <div className="flex items-center justify-end gap-3">

          <button
            ref={cancelRef}
            type="button"
            onClick={onCancel}
            disabled={loading}
            className="inline-flex items-center justify-center text-sm font-medium border-2 border-vault-border rounded-swiss px-5 py-2.5 bg-vault-surface text-vault-text hover:bg-vault-secondary transition-colors disabled:opacity-50 disabled:pointer-events-none"
          >
            Cancel
          </button>

          <button
            type="button"
            onClick={onConfirm}
            disabled={loading}
            className="inline-flex items-center justify-center gap-2 bg-vault-error text-white text-sm font-medium border-2 border-vault-border rounded-brutal px-5 py-2.5 shadow-brutal transition-transform duration-100 active:shadow-none active:translate-x-[3px] active:translate-y-[3px] disabled:opacity-60 disabled:pointer-events-none"
          >
            {loading ? (
              <Loader2
                size={15}
                className="animate-spin"
              />
            ) : (
              <Trash2 size={15} />
            )}

            {loading
              ? "Deleting…"
              : "Delete document"}
          </button>

        </div>

        {/* Audit message */}

        <p className="text-xs text-vault-muted mt-4">
          This action is recorded in the
          audit log.
        </p>
      </div>
    </div>
  );
}