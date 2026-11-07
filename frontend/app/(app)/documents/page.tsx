"use client";

import {
  useEffect,
  useRef,
  useState,
} from "react";
import Link from "next/link";

import { TopBar } from "@/components/TopBar";
import { SwissCard } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { StatusBadge } from "@/components/ui/StatusBadge";
import { ConfirmDeleteDialog } from "@/components/ui/ConfirmDeleteDialog";

import {
  getDocuments,
  uploadDocument,
  getDocumentFile,
  deleteDocument,
  DocumentResponse,
} from "@/lib/api";

import {
  Upload,
  FileText,
  Search,
  ArrowRight,
  X,
  Trash2,
} from "lucide-react";

export default function DocumentsPage() {
  const fileInputRef =
    useRef<HTMLInputElement>(null);

  const [documents, setDocuments] =
    useState<DocumentResponse[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [uploading, setUploading] =
    useState(false);

  const [uploadMessage, setUploadMessage] =
    useState("");

  const [error, setError] =
    useState<string | null>(null);

  const [searchQuery, setSearchQuery] =
    useState("");

  const [selectedDocument, setSelectedDocument] =
    useState<DocumentResponse | null>(null);

  const [documentUrl, setDocumentUrl] =
    useState<string | null>(null);

  const [loadingPreview, setLoadingPreview] =
    useState(false);

  /*
   * Only represents the document that is
   * currently being uploaded/processed.
   */
  const [processingUploadName, setProcessingUploadName] =
    useState<string | null>(null);

  /*
   * Document waiting for delete confirmation.
   */
  const [toDelete, setToDelete] =
    useState<DocumentResponse | null>(null);

  /*
   * True while the DELETE request is running.
   */
  const [deleting, setDeleting] =
    useState(false);

  /* ---------------- LOAD DOCUMENTS ---------------- */

  const loadDocuments = async () => {
    try {
      setLoading(true);
      setError(null);

      const data =
        await getDocuments();

      setDocuments(data);
    } catch (error) {
      // The session overlay handles missing or expired authentication.
      // Do not log this expected condition as an application error.
      if (
        error instanceof Error &&
        error.message === "You are not authenticated"
      ) {
        return;
      }

      console.error("Failed to load documents:", error);

      setError(
        error instanceof Error
          ? error.message
          : "Failed to load documents."
      );
    } finally {
      setLoading(false);
    }

  };

  useEffect(() => {
    loadDocuments();
  }, []);

  /* ---------------- CLEAN PDF URL ---------------- */

  useEffect(() => {
    return () => {
      if (documentUrl) {
        URL.revokeObjectURL(
          documentUrl
        );
      }
    };
  }, [documentUrl]);

  /* ---------------- UPLOAD ---------------- */

  const handleUploadClick = () => {
    fileInputRef.current?.click();
  };

  const handleFileSelected = async (
    event: React.ChangeEvent<HTMLInputElement>
  ) => {
    const file =
      event.target.files?.[0];

    if (!file) {
      return;
    }

    setUploading(true);
    setProcessingUploadName(
      file.name
    );
    setUploadMessage("");
    setError(null);

    try {
      await uploadDocument(file);

      setUploadMessage(
        `"${file.name}" uploaded and indexed successfully.`
      );

      await loadDocuments();
    } catch (error) {
      console.error(
        "Upload failed:",
        error
      );

      setUploadMessage(
        error instanceof Error
          ? error.message
          : "Failed to upload document."
      );
    } finally {
      setUploading(false);

      setProcessingUploadName(
        null
      );

      event.target.value = "";
    }
  };

  /* ---------------- OPEN PDF ---------------- */

  const handleDocumentClick = async (
    document: DocumentResponse
  ) => {
    if (document.status !== "Ready") {
      return;
    }

    try {
      setLoadingPreview(true);
      setError(null);

      if (documentUrl) {
        URL.revokeObjectURL(
          documentUrl
        );

        setDocumentUrl(null);
      }

      const blob =
        await getDocumentFile(
          document.id
        );

      const url =
        URL.createObjectURL(blob);

      setSelectedDocument(
        document
      );

      setDocumentUrl(url);
    } catch (error) {
      
      if (
    error instanceof Error &&
    error.message === "You are not authenticated"
  ) {
    return;
  }
      
      console.error(
        "Failed to open document:",
        error
      );

      setError(
        error instanceof Error
          ? error.message
          : "Failed to open document."
      );
    } finally {
      setLoadingPreview(false);
    }
  };

  /* ---------------- CLOSE PDF ---------------- */

  const handleClosePreview = () => {
    if (documentUrl) {
      URL.revokeObjectURL(
        documentUrl
      );
    }

    setDocumentUrl(null);
    setSelectedDocument(null);
  };

  /* ---------------- OPEN DELETE DIALOG ---------------- */

  const handleDeleteClick = (
    event: React.MouseEvent,
    document: DocumentResponse
  ) => {
    /*
     * Prevent the card click from opening
     * the PDF viewer.
     */
    event.stopPropagation();

    setError(null);

    setToDelete(document);
  };

  /* ---------------- CANCEL DELETE ---------------- */

  const handleCancelDelete = () => {
    if (deleting) {
      return;
    }

    setToDelete(null);
  };

  /* ---------------- CONFIRM DELETE ---------------- */

  const handleConfirmDelete = async () => {
    if (!toDelete) {
      return;
    }

    try {
      setDeleting(true);
      setError(null);

      await deleteDocument(
        toDelete.id
      );

      /*
       * If the deleted document is currently
       * open in the PDF viewer, close it.
       */
      if (
        selectedDocument?.id ===
        toDelete.id
      ) {
        handleClosePreview();
      }

      /*
       * Refresh the list from the backend.
       */
      await loadDocuments();

      setUploadMessage(
        `"${toDelete.name}" removed successfully.`
      );

      /*
       * Close the confirmation dialog.
       */
      setToDelete(null);
    } catch (error) {
      console.error(
        "Failed to remove document:",
        error
      );

      setError(
        error instanceof Error
          ? error.message
          : "Failed to remove document."
      );
    } finally {
      setDeleting(false);
    }
  };

  /* ---------------- SEARCH ---------------- */

  const filteredDocuments =
    documents.filter((document) =>
      document.name
        .toLowerCase()
        .includes(
          searchQuery
            .trim()
            .toLowerCase()
        )
    );

  const readyDocuments =
    filteredDocuments.filter(
      (document) =>
        document.status ===
        "Ready"
    );

  const failedDocuments =
    filteredDocuments.filter(
      (document) =>
        document.status ===
        "Failed"
    );

  return (
    <>
      <TopBar
        title="Documents"
        subtitle="The knowledge base behind every answer in Ask"
      />

      <div className="p-8 max-w-6xl space-y-6">

        {/* ---------------- HEADER ---------------- */}

        <div className="flex items-center justify-between gap-4">

          <div className="flex items-center gap-2 border border-vault-border/20 rounded-swiss px-3 py-2 text-sm text-vault-muted w-80">

            <Search size={15} />

            <input
              type="text"
              value={searchQuery}
              onChange={(event) =>
                setSearchQuery(
                  event.target.value
                )
              }
              placeholder="Search documents..."
              className="w-full bg-transparent outline-none text-vault-text placeholder:text-vault-muted"
            />

          </div>

          <div className="flex items-center gap-3">

            <Link
              href="/ask"
              className="hidden sm:inline-flex items-center gap-1 text-sm text-vault-muted hover:text-vault-text"
            >
              Back to Ask
              <ArrowRight size={13} />
            </Link>

            <input
              ref={fileInputRef}
              type="file"
              accept="application/pdf,.pdf"
              className="hidden"
              onChange={
                handleFileSelected
              }
            />

            <Button
              variant="brutal"
              icon={
                <Upload size={15} />
              }
              onClick={
                handleUploadClick
              }
              disabled={uploading}
            >
              {uploading
                ? "Uploading..."
                : "Upload document"}
            </Button>

          </div>

        </div>

        {/* ---------------- MESSAGE ---------------- */}

        {uploadMessage && (
          <p className="text-sm text-vault-muted">
            {uploadMessage}
          </p>
        )}

        {/* ---------------- ERROR ---------------- */}

        {error && (
          <SwissCard className="p-4">
            <p className="text-sm text-red-600">
              {error}
            </p>
          </SwissCard>
        )}

        {/* ---------------- CURRENT PROCESSING ---------------- */}

        {processingUploadName && (
          <div className="space-y-3">

            <div>
              <p className="text-sm font-medium">
                Processing
              </p>

              <p className="text-xs text-vault-muted mt-1">
                Your document is being
                extracted, chunked and indexed.
              </p>
            </div>

            <SwissCard className="p-5">

              <div className="flex items-center justify-between gap-4">

                <div className="flex items-center gap-3 min-w-0">

                  <FileText
                    size={18}
                    className="shrink-0 text-vault-muted"
                  />

                  <div className="min-w-0">

                    <p className="text-sm font-medium truncate">
                      {processingUploadName}
                    </p>

                    <p className="text-xs text-vault-muted mt-1">
                      Processing document...
                    </p>

                  </div>

                </div>

                <StatusBadge
                  status="Processing"
                />

              </div>

              <div className="mt-4">

                <div className="h-1.5 bg-vault-border/10 rounded-full overflow-hidden">

                  <div
                    className="h-full bg-vault-text animate-pulse"
                    style={{
                      width: "65%",
                    }}
                  />

                </div>

                <p className="text-xs text-vault-muted mt-2">
                  Extracting text,
                  creating chunks and
                  indexing...
                </p>

              </div>

            </SwissCard>

          </div>
        )}

        {/* ---------------- PDF VIEWER ---------------- */}

        {selectedDocument && (
          <SwissCard className="p-0 overflow-hidden">

            <div className="flex items-center justify-between gap-4 p-4 border-b border-vault-border/20">

              <div className="min-w-0">

                <p className="text-sm font-medium truncate">
                  {selectedDocument.name}
                </p>

                <p className="text-xs text-vault-muted mt-1">
                  PDF ·{" "}
                  {formatFileSize(
                    selectedDocument.fileSize
                  )}
                </p>

              </div>

              <button
                type="button"
                onClick={
                  handleClosePreview
                }
                className="flex items-center gap-1 text-sm text-vault-muted hover:text-vault-text shrink-0"
              >
                <X size={15} />
                Close
              </button>

            </div>

            <div className="h-[700px]">

              {loadingPreview ? (
                <div className="h-full flex items-center justify-center">
                  <p className="text-sm text-vault-muted">
                    Opening document...
                  </p>
                </div>
              ) : documentUrl ? (
                <iframe
                  src={documentUrl}
                  title={
                    selectedDocument.name
                  }
                  className="w-full h-full border-0"
                />
              ) : null}

            </div>

          </SwissCard>
        )}

        {/* ---------------- DOCUMENT LIST ---------------- */}

        {loading ? (

          <SwissCard className="p-8">
            <p className="text-sm text-vault-muted">
              Loading documents...
            </p>
          </SwissCard>

        ) : documents.length === 0 ? (

          <SwissCard className="p-8">

            <div className="text-center">

              <FileText
                size={28}
                className="mx-auto mb-3 text-vault-muted"
              />

              <p className="text-sm font-medium">
                No documents yet
              </p>

              <p className="text-xs text-vault-muted mt-1">
                Upload a PDF to start
                building your knowledge vault.
              </p>

            </div>

          </SwissCard>

        ) : filteredDocuments.length === 0 ? (

          <SwissCard className="p-8">

            <div className="text-center">

              <Search
                size={28}
                className="mx-auto mb-3 text-vault-muted"
              />

              <p className="text-sm font-medium">
                No matching documents
              </p>

              <p className="text-xs text-vault-muted mt-1">
                Try a different document name.
              </p>

            </div>

          </SwissCard>

        ) : (

          <div className="space-y-6">

            {/* ---------------- READY ---------------- */}

            {readyDocuments.length > 0 && (
              <div className="space-y-3">

                <div>
                  <p className="text-sm font-medium">
                    Documents
                  </p>

                  <p className="text-xs text-vault-muted mt-1">
                    Click a document to open its PDF.
                  </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

                  {readyDocuments.map(
                    (doc) => (

                      <SwissCard
                        key={doc.id}
                        className="p-5 cursor-pointer hover:border-vault-border transition-colors"
                        onClick={() =>
                          handleDocumentClick(
                            doc
                          )
                        }
                      >

                        <div className="flex items-start justify-between gap-3">

                          <div className="flex items-start gap-3 min-w-0">

                            <FileText
                              size={18}
                              className="mt-0.5 shrink-0 text-vault-muted"
                            />

                            <div className="min-w-0">

                              <p className="text-sm font-medium truncate">
                                {doc.name}
                              </p>

                              <p className="text-xs text-vault-muted mt-0.5">
                                PDF ·{" "}
                                {formatFileSize(
                                  doc.fileSize
                                )}
                              </p>

                              <p className="text-xs text-vault-muted mt-2">
                                Uploaded{" "}
                                {formatDate(
                                  doc.createdAt
                                )}
                              </p>

                            </div>

                          </div>

                          <StatusBadge
                            status="Ready"
                          />

                        </div>

                        <div className="flex items-center justify-between gap-3 mt-4">

                          <p className="text-xs text-vault-muted">
                            Click to open
                          </p>

                          <button
                            type="button"
                            onClick={(event) =>
                              handleDeleteClick(
                                event,
                                doc
                              )
                            }
                            disabled={deleting}
                            className="inline-flex items-center gap-1.5 text-xs text-vault-error hover:underline disabled:opacity-50"
                          >

                            <Trash2
                              size={13}
                            />

                            Remove

                          </button>

                        </div>

                      </SwissCard>

                    )
                  )}

                </div>

              </div>
            )}

            {/* ---------------- FAILED ---------------- */}

            {failedDocuments.length > 0 && (
              <div className="space-y-3">

                <div>
                  <p className="text-sm font-medium">
                    Failed
                  </p>

                  <p className="text-xs text-vault-muted mt-1">
                    These documents could not be
                    processed successfully.
                  </p>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">

                  {failedDocuments.map(
                    (doc) => (

                      <SwissCard
                        key={doc.id}
                        className="p-5"
                      >

                        <div className="flex items-start justify-between gap-3">

                          <div className="flex items-start gap-3 min-w-0">

                            <FileText
                              size={18}
                              className="mt-0.5 shrink-0 text-vault-muted"
                            />

                            <div className="min-w-0">

                              <p className="text-sm font-medium truncate">
                                {doc.name}
                              </p>

                              <p className="text-xs text-vault-muted mt-0.5">
                                PDF ·{" "}
                                {formatFileSize(
                                  doc.fileSize
                                )}
                              </p>

                              <p className="text-xs text-vault-muted mt-2">
                                Uploaded{" "}
                                {formatDate(
                                  doc.createdAt
                                )}
                              </p>

                            </div>

                          </div>

                          <StatusBadge
                            status="Failed"
                          />

                        </div>

                        <div className="flex items-center justify-between gap-3 mt-4">

                          <p className="text-xs text-vault-error">
                            Processing failed
                          </p>

                          <button
                            type="button"
                            onClick={(event) =>
                              handleDeleteClick(
                                event,
                                doc
                              )
                            }
                            disabled={deleting}
                            className="inline-flex items-center gap-1.5 text-xs text-vault-error hover:underline disabled:opacity-50"
                          >

                            <Trash2
                              size={13}
                            />

                            Remove

                          </button>

                        </div>

                      </SwissCard>

                    )
                  )}

                </div>

              </div>
            )}

          </div>

        )}

      </div>

      {/* ---------------- DELETE CONFIRMATION ---------------- */}

      <ConfirmDeleteDialog
        open={toDelete !== null}
        documentName={
          toDelete?.name ?? ""
        }
        loading={deleting}
        onCancel={
          handleCancelDelete
        }
        onConfirm={
          handleConfirmDelete
        }
      />
    </>
  );
}

/* ---------------- HELPERS ---------------- */

function formatFileSize(
  bytes: number
): string {
  if (bytes < 1024) {
    return `${bytes} B`;
  }

  if (bytes < 1024 * 1024) {
    return `${(
      bytes / 1024
    ).toFixed(1)} KB`;
  }

  return `${(
    bytes /
    (1024 * 1024)
  ).toFixed(1)} MB`;
}

function formatDate(
  value: string
): string {
  return new Date(
    value
  ).toLocaleDateString(
    "en-US",
    {
      month: "short",
      day: "numeric",
      year: "numeric",
    }
  );
}