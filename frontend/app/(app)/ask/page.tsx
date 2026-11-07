"use client";

import {
  useEffect,
  useRef,
  useState,
} from "react";

import {
  streamRag,
  getDocumentFile,
  getDocuments,
  type DocumentResponse,
  type RagSource,
} from "@/lib/api";

import Link from "next/link";

import { TopBar } from "@/components/TopBar";
import { Button } from "@/components/ui/Button";

import {
  BrutalCard,
  SwissCard,
} from "@/components/ui/Card";

import { StatusBadge } from "@/components/ui/StatusBadge";

import {
  ArrowUp,
  ArrowRight,
  FileText,
  Loader2,
  TriangleAlert,
  PanelRightClose,
  PanelRightOpen,
} from "lucide-react";


const suggestions = [
  "What documents are required to file this claim?",
  "What are the pre-authorization requirements for an outpatient MRI?",
  "How long does a payer have to respond to a reimbursement appeal?",
];


export default function AskPage() {
  const [question, setQuestion] =
    useState("");

  const [state, setState] = useState<
    "idle" | "loading" | "answered" | "error"
  >("idle");

  const [answer, setAnswer] =
    useState("");

  const [sources, setSources] =
    useState<RagSource[]>([]);

  const [selectedSource, setSelectedSource] =
    useState<RagSource | null>(null);

  const [pdfUrl, setPdfUrl] =
    useState<string | null>(null);

  const [pdfLoading, setPdfLoading] =
    useState(false);

  const [pdfError, setPdfError] =
    useState("");

  const [error, setError] =
    useState("");

  const [documents, setDocuments] =
    useState<DocumentResponse[]>([]);

  const [documentsLoading, setDocumentsLoading] =
    useState(true);

  /*
   * null = All documents
   * UUID = selected document
   * "" = no document selected yet
   */
  const [selectedDocumentId, setSelectedDocumentId] =
    useState<string>("");

  const [railWidth, setRailWidth] =
    useState(520);

  const [railCollapsed, setRailCollapsed] =
    useState(false);

  const resizing =
    useRef(false);


  /*
   * Load user's documents.
   */
  useEffect(() => {
    let cancelled = false;

    async function loadDocuments() {
      try {
        setDocumentsLoading(true);

        const data =
          await getDocuments();

        if (!cancelled) {
          setDocuments(data);
        }
      
} catch (error) {
  if (
    error instanceof Error &&
    error.message === "You are not authenticated"
  ) {
    return;
  }

  console.error("Failed to load documents:", error);

  // Keep your existing error handling here.
} finally {
  if (!cancelled) {
    setDocumentsLoading(false);
  }
}

    }

    loadDocuments();

    return () => {
      cancelled = true;
    };
  }, []);


  /*
   * Load the selected PDF.
   */
  useEffect(() => {
    if (!selectedSource) {
      setPdfUrl(null);
      setPdfError("");
      setPdfLoading(false);

      return;
    }

    let objectUrl: string | null = null;

    async function loadPdf() {
      try {
        setPdfLoading(true);
        setPdfError("");
        setPdfUrl(null);

        const blob =
          await getDocumentFile(
            selectedSource!.documentId
          );

        objectUrl =
          URL.createObjectURL(blob);

        const page =
          selectedSource!.pageNumber ?? 1;

        setPdfUrl(
          `${objectUrl}#page=${page}`
        );
      } catch (error) {
        setPdfError(
          error instanceof Error
            ? error.message
            : "Failed to load PDF"
        );
      } finally {
        setPdfLoading(false);
      }
    }

    loadPdf();

    return () => {
      if (objectUrl) {
        URL.revokeObjectURL(
          objectUrl
        );
      }
    };
  }, [selectedSource]);


  /*
   * Resize the right context panel.
   */
  function startResize() {
    resizing.current = true;

    document.body.style.cursor =
      "col-resize";

    document.body.style.userSelect =
      "none";

    function handleMouseMove(
      event: MouseEvent
    ) {
      if (!resizing.current) {
        return;
      }

      const newWidth =
        window.innerWidth -
        event.clientX;

      const minWidth = 420;
      const maxWidth = 760;

      setRailWidth(
        Math.min(
          maxWidth,
          Math.max(
            minWidth,
            newWidth
          )
        )
      );
    }

    function stopResize() {
      resizing.current = false;

      document.body.style.cursor =
        "";

      document.body.style.userSelect =
        "";

      window.removeEventListener(
        "mousemove",
        handleMouseMove
      );

      window.removeEventListener(
        "mouseup",
        stopResize
      );
    }

    window.addEventListener(
      "mousemove",
      handleMouseMove
    );

    window.addEventListener(
      "mouseup",
      stopResize
    );
  }


  /*
   * Reset the current answer when
   * the search scope changes.
   */
  function handleDocumentChange(
    documentId: string
  ) {
    setSelectedDocumentId(
      documentId
    );

    setState("idle");
    setAnswer("");
    setSources([]);
    setSelectedSource(null);
    setPdfUrl(null);
    setPdfError("");
    setError("");
  }


  /*
   * Ask VaultRAG a question.
   */
  async function ask(q?: string) {
    if (state === "loading") {
      return;
    }

    const value =
      q ?? question;

    if (!value.trim()) {
      return;
    }

    /*
     * Require the user to explicitly choose
     * a search scope.
     */
    if (!selectedDocumentId) {
      setError(
        "Please select a document or choose All documents."
      );

      setState("error");

      return;
    }

    setQuestion(value);

    setState("loading");

    setError("");

    setAnswer("");

    setSources([]);

    setSelectedSource(null);

    setPdfUrl(null);

    setPdfError("");

    try {
      await streamRag(
        value,
        8,

        /*
         * Streaming answer chunks.
         */
        (chunk) => {
          setAnswer(
            (current) =>
              current + chunk
          );
        },

        /*
         * Sources arrive before
         * the answer generation.
         */
        (sources) => {
          console.log(
            "VAULTRAG SOURCES:",
            JSON.stringify(
              sources,
              null,
              2
            )
          );

          setSources(sources);
        },

        /*
         * Empty string means All documents.
         */
        selectedDocumentId ===
          "__ALL_DOCUMENTS__"
          ? null
          : selectedDocumentId
      );

      setState("answered");
    } catch (error) {
      setError(
        error instanceof Error
          ? error.message
          : "Something went wrong while getting an answer."
      );

      setState("error");
    }
  }


  /*
   * Ready documents only.
   */
  const readyDocuments = documents.filter(
  (document) => document.status?.toUpperCase() === "READY"
);

const failedDocuments = documents.filter(
  (document) => document.status?.toUpperCase() === "FAILED"
);


  const selectedDocument =
    readyDocuments.find(
      (document) =>
        document.id ===
        selectedDocumentId
    );


  const searchScopeLabel =
    selectedDocumentId ===
    "__ALL_DOCUMENTS__"
      ? "All documents"
      : selectedDocument
        ? selectedDocument.name
        : "Select a document";


  return (
    <>
      <TopBar
        title="Ask VaultRAG"
        subtitle="Private document intelligence workspace"
      />

      <div className="flex">

        {/* ========================= */}
        {/* MAIN CONTENT               */}
        {/* ========================= */}

        <div className="flex-1 min-w-0 p-8">

          {/* INTRO */}

          {state === "idle" && (
            <div className="mb-8">

              <h2 className="font-serif text-3xl leading-snug mb-2">
                Ask your documents
              </h2>

              <p className="text-sm text-vault-muted">
                Select a document to ask
                questions specifically about
                that PDF, or choose All
                documents to search your
                entire secure workspace.
              </p>

            </div>
          )}


          {/* SEARCH SCOPE */}

          <div className="mb-4">

            <label
              htmlFor="document-scope"
              className="block text-xs font-mono tracking-wide text-vault-muted mb-2"
            >
              SEARCH IN
            </label>

            <select
              id="document-scope"
              value={selectedDocumentId}
              onChange={(event) =>
                handleDocumentChange(
                  event.target.value
                )
              }
              disabled={
                state === "loading" ||
                documentsLoading
              }
              className="w-full border border-vault-border/25 rounded-swiss bg-vault-surface px-3 py-3 text-sm outline-none focus:border-vault-border/50 disabled:opacity-60"
            >
              <option value="">
                Select a document
              </option>

              <option value="__ALL_DOCUMENTS__">
                All documents
              </option>

              {readyDocuments.map(
                (document) => (
                  <option
                    key={document.id}
                    value={document.id}
                  >
                    {document.name}
                  </option>
                )
              )}
            </select>

            {documentsLoading && (
              <div className="mt-2 flex items-center gap-2 text-xs text-vault-muted">
                <Loader2
                  size={13}
                  className="animate-spin"
                />
                Loading documents…
              </div>
            )}

            {!documentsLoading &&
              readyDocuments.length === 0 && (
                <p className="mt-2 text-xs text-vault-muted">
                  No ready documents are
                  available yet. Upload a PDF
                  from Documents first.
                </p>
              )}

            {!documentsLoading &&
              selectedDocumentId ===
                "__ALL_DOCUMENTS__" && (
                <p className="mt-2 text-xs text-vault-muted">
                  Searching across all ready
                  documents in your workspace.
                </p>
              )}

            {!documentsLoading &&
              selectedDocument && (
                <p className="mt-2 text-xs text-vault-muted truncate">
                  Questions will be answered
                  using only{" "}
                  <span className="text-vault-text">
                    {selectedDocument.name}
                  </span>
                  .
                </p>
              )}

          </div>


          {/* QUESTION INPUT */}

          <form
            onSubmit={(e) => {
              e.preventDefault();
              ask();
            }}
            className="flex items-center gap-2 border border-vault-border/25 rounded-swiss bg-vault-surface p-2 pl-4"
          >

            <input
              value={question}
              onChange={(e) =>
                setQuestion(
                  e.target.value
                )
              }
              placeholder="What are the requirements for…"
              disabled={
                state === "loading"
              }
              className="flex-1 min-w-0 bg-transparent text-sm outline-none placeholder:text-vault-muted disabled:opacity-60"
            />

            <Button
              type="submit"
              variant="brutal"
              disabled={
                state === "loading" ||
                !question.trim() ||
                !selectedDocumentId
              }
              icon={
                state === "loading" ? (
                  <Loader2
                    size={15}
                    className="animate-spin"
                  />
                ) : (
                  <ArrowUp size={15} />
                )
              }
            >
              Ask
            </Button>

          </form>


          {/* SUGGESTIONS */}

          {state === "idle" &&
            selectedDocumentId && (
              <div className="mt-4 flex flex-col gap-2">

                {suggestions.map(
                  (suggestion) => (
                    <button
                      key={suggestion}
                      type="button"
                      onClick={() =>
                        ask(suggestion)
                      }
                      className="text-left text-sm text-vault-muted px-3 py-2 rounded-swiss hover:text-vault-text hover:bg-vault-secondary transition-colors"
                    >
                      {suggestion}
                    </button>
                  )
                )}

              </div>
            )}


          {/* SEARCHING */}

          {state === "loading" &&
            !answer && (
              <div className="mt-10 flex items-center gap-2 text-sm text-vault-muted">

                <Loader2
                  size={15}
                  className="animate-spin"
                />

                Searching{" "}
                <span className="truncate max-w-[300px]">
                  {searchScopeLabel}
                </span>
                …

              </div>
            )}


          {/* ERROR */}

          {state === "error" && (
            <div className="mt-10 border border-vault-error/30 bg-vault-error-bg rounded-swiss p-4">

              <p className="text-sm font-medium">
                Unable to answer your
                question
              </p>

              <p className="text-sm text-vault-text/70 mt-1">
                {error}
              </p>

            </div>
          )}


          {/* ANSWER + SOURCES */}

          {(state === "loading" ||
            state === "answered") &&
            answer && (

              <div className="mt-10 space-y-8">

                {/* ANSWER */}

                <div className="min-w-0">

                  <p className="text-xs font-mono tracking-wide text-vault-muted mb-2">
                    ANSWER
                  </p>

                  <p className="font-serif text-[19px] leading-relaxed text-vault-text max-w-measure whitespace-pre-wrap break-words">
                    {answer}
                  </p>

                  {state === "loading" && (
                    <div className="mt-3 flex items-center gap-2 text-xs text-vault-muted">

                      <Loader2
                        size={13}
                        className="animate-spin"
                      />

                      Generating answer…

                    </div>
                  )}

                </div>


                {/* SOURCES */}

                <div>

                  <p className="text-xs font-mono tracking-wide text-vault-muted mb-3">
                    SOURCES
                  </p>

                  <div className="grid gap-3">

                    {sources.length > 0 ? (
                      sources.map(
                        (source) => (
                          <button
                            key={
                              source.chunkId
                            }
                            type="button"
                            onClick={() => {
                              setSelectedSource(
                                source
                              );

                              setRailCollapsed(
                                false
                              );
                            }}
                            className="w-full text-left"
                          >

                            <BrutalCard className="p-4 flex items-center justify-between gap-4 hover:bg-vault-secondary transition-colors cursor-pointer">

                              <div className="flex items-center gap-3 min-w-0">

                                <FileText
                                  size={17}
                                  className="shrink-0"
                                />

                                <div className="min-w-0">

                                  <p className="text-sm font-medium truncate">
                                    {
                                      source.documentName
                                    }
                                  </p>

                                  <p className="text-xs text-vault-text/60">
                                    Page{" "}
                                    {
                                      source.pageNumber ??
                                      "Unknown"
                                    }
                                  </p>

                                </div>

                              </div>

                              <span className="text-xs font-mono text-vault-text/50 shrink-0">
                                p.
                                {source.pageNumber ??
                                  "?"}{" "}
                                →
                              </span>

                            </BrutalCard>

                          </button>
                        )
                      )
                    ) : (
                      <p className="text-sm text-vault-muted">
                        Sources will appear
                        here after the answer
                        is generated.
                      </p>
                    )}

                  </div>

                </div>

              </div>
            )}

        </div>


        {/* ========================= */}
        {/* COLLAPSED PANEL BUTTON     */}
        {/* ========================= */}

        {railCollapsed && (
          <button
            type="button"
            onClick={() =>
              setRailCollapsed(
                false
              )
            }
            aria-label="Open context panel"
            title="Open context panel"
            className="hidden lg:flex fixed right-4 top-20 z-40 w-9 h-9 items-center justify-center bg-vault-surface border border-vault-border/15 rounded-swiss hover:bg-vault-secondary transition-colors"
          >
            <PanelRightOpen
              size={16}
            />
          </button>
        )}


        {/* ========================= */}
        {/* RIGHT CONTEXT PANEL        */}
        {/* ========================= */}

        {!railCollapsed && (
          <>

            {/* RESIZE HANDLE */}

            <div
              onMouseDown={
                startResize
              }
              className="hidden lg:flex w-2 shrink-0 cursor-col-resize items-center justify-center group"
              aria-label="Resize context panel"
            >
              <div className="h-full w-px bg-vault-border/10 group-hover:bg-vault-border/30 transition-colors" />
            </div>


            {/* PANEL */}

            <aside
              className="hidden lg:flex shrink-0 border-l border-vault-border/15 p-6 flex-col gap-6 min-h-[calc(100vh-4rem)] overflow-y-auto relative"
              style={{
                width: `${railWidth}px`,
              }}
            >

              {/* COLLAPSE BUTTON */}

              <button
                type="button"
                onClick={() =>
                  setRailCollapsed(
                    true
                  )
                }
                aria-label="Collapse context panel"
                title="Collapse panel"
                className="absolute top-4 left-3 w-7 h-7 flex items-center justify-center rounded-swiss text-vault-muted hover:bg-vault-secondary hover:text-vault-text transition-colors"
              >
                <PanelRightClose
                  size={15}
                />
              </button>


              <div className="pt-8">

                {/* SELECTED SOURCE */}

                {selectedSource && (
                  <div className="mb-6">

                    <div className="flex items-start justify-between gap-3 mb-4">

                      <div className="min-w-0">

                        <p className="text-xs font-mono tracking-wide text-vault-muted">
                          SOURCE
                        </p>

                        <p className="text-sm font-medium truncate mt-1">
                          {
                            selectedSource.documentName
                          }
                        </p>

                        <p className="text-xs text-vault-muted mt-1">
                          Page{" "}
                          {
                            selectedSource.pageNumber ??
                            "Unknown"
                          }
                        </p>

                      </div>

                      <button
                        type="button"
                        onClick={() => {
                          setSelectedSource(
                            null
                          );

                          setPdfUrl(null);

                          setPdfError("");
                        }}
                        className="shrink-0 text-xs text-vault-muted hover:text-vault-text"
                      >
                        Close
                      </button>

                    </div>


                    {/* PDF LOADING */}

                    {pdfLoading && (
                      <div className="h-[600px] border border-vault-border/15 rounded-swiss flex items-center justify-center bg-vault-surface">

                        <div className="flex items-center gap-2 text-sm text-vault-muted">

                          <Loader2
                            size={15}
                            className="animate-spin"
                          />

                          Loading document…

                        </div>

                      </div>
                    )}


                    {/* PDF ERROR */}

                    {pdfError && (
                      <div className="border border-vault-error/30 bg-vault-error-bg rounded-swiss p-4">

                        <p className="text-sm font-medium">
                          Unable to open
                          document
                        </p>

                        <p className="text-xs text-vault-text/70 mt-1 break-words">
                          {pdfError}
                        </p>

                      </div>
                    )}


                    {/* PDF VIEWER */}

                    {pdfUrl &&
                      !pdfLoading &&
                      !pdfError && (

                        <div className="w-full min-w-0 overflow-hidden border border-vault-border/15 rounded-swiss bg-vault-surface">

                          <iframe
                            src={pdfUrl}
                            title={
                              selectedSource.documentName
                            }
                            className="block w-full h-[650px] border-0"
                          />

                        </div>
                      )}

                  </div>
                )}


                {/* DEFAULT CONTEXT PANEL */}

                {!selectedSource && (
                  <>

                    {/* SEARCH SCOPE SUMMARY */}

                    <SwissCard className="p-4">

                      <p className="text-xs text-vault-muted mb-3">
                        Search scope
                      </p>

                      <p className="text-sm font-medium break-words">
                        {searchScopeLabel}
                      </p>

                      <p className="text-xs text-vault-muted mt-2">
                        {selectedDocumentId ===
                        "__ALL_DOCUMENTS__"
                          ? "RAG will search all ready documents."
                          : selectedDocument
                            ? "RAG will search only this document."
                            : "Choose a document before asking a question."}
                      </p>

                    </SwissCard>


                    {/* VAULT SUMMARY */}

                    <SwissCard className="p-4 mt-6">

                      <p className="text-xs text-vault-muted mb-3">
                        Your vault
                      </p>

                      <div className="grid grid-cols-2 gap-3">

                        <div>

                          <p className="text-2xl font-medium">
                            {
                              readyDocuments.length
                            }
                          </p>

                          <p className="text-xs text-vault-muted">
                            docs ready
                          </p>

                        </div>

                        <div>

                          <p className="text-2xl font-medium">
                            {documents.length}
                          </p>

                          <p className="text-xs text-vault-muted">
                            total docs
                          </p>

                        </div>

                      </div>

                    </SwissCard>


                    {/* FAILED DOCUMENTS */}

                    {failedDocuments.length >
                      0 && (
                      <BrutalCard
                        tone="error"
                        className="p-4 mt-6"
                      >

                        <div className="flex items-start gap-2.5">

                          <TriangleAlert
                            size={16}
                            className="mt-0.5 shrink-0"
                          />

                          <div className="min-w-0">

                            <p className="text-sm font-medium">
                              {
                                failedDocuments.length
                              }{" "}
                              document
                              {failedDocuments.length >
                              1
                                ? "s"
                                : ""}{" "}
                              failed to
                              process
                            </p>

                            <p className="text-xs text-vault-text/70 mt-1 truncate">
                              {
                                failedDocuments[0]
                                  .name
                              }
                            </p>

                            <Link
                              href="/documents"
                              className="inline-flex items-center gap-1 text-xs font-medium mt-2 hover:underline"
                            >
                              Review in
                              Documents

                              <ArrowRight
                                size={11}
                              />
                            </Link>

                          </div>

                        </div>

                      </BrutalCard>
                    )}


                    {/* DOCUMENTS */}

                    <SwissCard className="p-0 overflow-hidden mt-6">

                      <div className="flex items-center justify-between px-4 py-3 border-b border-vault-border/10">

                        <p className="text-xs text-vault-muted">
                          Documents
                        </p>

                        <Link
                          href="/documents"
                          className="text-xs text-vault-muted hover:text-vault-text"
                        >
                          View all
                        </Link>

                      </div>

                      {documents
                        .slice(0, 4)
                        .map(
                          (doc) => (
                            <div
                              key={doc.id}
                              className="flex items-center justify-between gap-2 px-4 py-2.5 border-b border-vault-border/5 last:border-0 text-sm"
                            >

                              <span className="truncate">
                                {doc.name}
                              </span>

                              <StatusBadge
                                status={
                                  doc.status
                                }
                              />

                            </div>
                          )
                        )}

                    </SwissCard>


                    {/* RECENT ACTIVITY */}

                    <SwissCard className="p-4 mt-6">

                      <p className="text-xs text-vault-muted mb-2">
                        Recent activity
                      </p>

                      <Link
                        href="/audit"
                        className="text-xs text-vault-muted hover:text-vault-text"
                      >
                        View audit logs →
                      </Link>

                    </SwissCard>

                  </>
                )}

              </div>

            </aside>

          </>
        )}

      </div>
    </>
  );
}