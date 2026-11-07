export type DocStatus =
  | "Ready"
  | "Processing"
  | "Uploading"
  | "Failed";

export type VaultDocument = {
  id: string;
  name: string;
  pages: number;
  status: DocStatus;
  uploaded: string;
  category: string;
};

export const documents: VaultDocument[] = [
  { id: "d1", name: "Claims Policy.pdf", pages: 24, status: "Ready", uploaded: "Sep 20, 2026", category: "Claims Guidelines" },
  { id: "d2", name: "Reimbursement Rules.pdf", pages: 18, status: "Ready", uploaded: "Sep 19, 2026", category: "Reimbursement Policy" },
  { id: "d3", name: "Pre-Authorization Policy.pdf", pages: 12, status: "Ready", uploaded: "Sep 18, 2026", category: "Pre-Authorization Rules" },
  { id: "d4", name: "Billing SOP.pdf", pages: 31, status: "Processing", uploaded: "Sep 20, 2026", category: "Billing SOPs" },
  { id: "d5", name: "Payer Agreement — Meridian Health.pdf", pages: 46, status: "Ready", uploaded: "Sep 14, 2026", category: "Payer Agreements" },
  { id: "d6", name: "Denial Guidelines.pdf", pages: 9, status: "Failed", uploaded: "Sep 20, 2026", category: "Denial Guidelines" },
];

export type AuditEvent = {
  id: string;
  time: string;
  user: string;
  action: string;
  resource: string;
};

export const auditEvents: AuditEvent[] = [
  { id: "a1", time: "10:42", user: "j.alvarez", action: "DOCUMENT_UPLOADED", resource: "Billing SOP.pdf" },
  { id: "a2", time: "10:45", user: "j.alvarez", action: "DOCUMENT_ACCESSED", resource: "Claims Policy.pdf" },
  { id: "a3", time: "10:48", user: "j.alvarez", action: "RAG_QUERY_EXECUTED", resource: "Pre-Authorization Policy.pdf" },
  { id: "a4", time: "11:02", user: "admin", action: "DOCUMENT_ACCESSED", resource: "Payer Agreement — Meridian Health.pdf" },
  { id: "a5", time: "11:14", user: "r.chen", action: "RAG_QUERY_EXECUTED", resource: "Reimbursement Rules.pdf" },
  { id: "a6", time: "11:30", user: "admin", action: "AUDIT_LOG_VIEWED", resource: "—" },
  { id: "a7", time: "11:47", user: "r.chen", action: "DOCUMENT_UPLOAD_FAILED", resource: "Denial Guidelines.pdf" },
];

export const sampleAnswer = {
  question: "What are the pre-authorization requirements for an outpatient MRI?",
  answer:
    "Pre-authorization for an outpatient MRI requires the referring physician's clinical documentation, a statement of medical necessity, and any prior conservative-treatment records. Requests submitted without the necessity statement are returned as incomplete rather than denied outright, and the requesting provider has 10 business days to resubmit.",
  sources: [
    { doc: "Pre-Authorization Policy.pdf", page: 4, section: "§ 2.1 Imaging Requests" },
    { doc: "Claims Policy.pdf", page: 12, section: "§ 4.3 Incomplete Submissions" },
  ],
};
