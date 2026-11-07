import { DocStatus } from "./mock-data";

const API_BASE_URL =

   process.env.NEXT_PUBLIC_API_BASE_URL ||

   "http://localhost:8080";

/* ---------------- AUTH SESSION HANDLING ---------------- */

function notifyUnauthorized(): void {
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event("vaultrag:unauthorized"));
  }
}

function handleUnauthorized(response: Response): boolean {
  if (response.status !== 401) {
    return false;
  }

  notifyUnauthorized();
  return true;
}

/* ---------------- AUTH ---------------- */

export interface LoginRequest {

   username: string;

   password: string;

}

export interface RegisterRequest {

   username: string;

   password: string;

}

export interface LoginResponse {

   token: string;

   id: string;

   username: string;

   role: string;

}

export interface RegisterResponse {

   id: string;

   username: string;

   role: string;

}

export async function login(

   credentials: LoginRequest

): Promise<LoginResponse> {

   const response = await fetch(

      `${API_BASE_URL}/api/auth/login`,

      {

         method: "POST",

         headers: {

            "Content-Type": "application/json",

         },

         body: JSON.stringify(credentials),

      }

   );

   const responseText = await response.text();

   let data:

      | LoginResponse

      | { error?: string; message?: string };

   try {

      data = JSON.parse(responseText);

   } catch {

      throw new Error(

         responseText || "Invalid response from login API"

      );

   }

   if (!response.ok) {

      throw new Error(

         ("error" in data && data.error) ||

            ("message" in data && data.message) ||

            "Login failed"

      );

   }

   return data as LoginResponse;

}

export async function register(

   credentials: RegisterRequest

): Promise<RegisterResponse> {

   const response = await fetch(

      `${API_BASE_URL}/api/auth/register`,

      {

         method: "POST",

         headers: {

            "Content-Type": "application/json",

         },

         body: JSON.stringify(credentials),

      }

   );

   const responseText = await response.text();

   let data:

      | RegisterResponse

      | { error?: string; message?: string };

   try {

      data = JSON.parse(responseText);

   } catch {

      throw new Error(

         responseText ||

            "Invalid response from registration API"

      );

   }

   if (!response.ok) {

      throw new Error(

         ("error" in data && data.error) ||

            ("message" in data && data.message) ||

            "Account creation failed"

      );

   }

   return data as RegisterResponse;

}

/* ---------------- RAG ---------------- */

export interface RagSource {

   chunkId: string;

   documentId: string;

   documentName: string;

   chunkIndex: number;

   pageNumber: number | null;

   content: string;

}

export interface RagResponse {

   answer: string;

   sources: RagSource[];

}

export async function askRag(

   question: string,

   limit = 8,

   documentId?: string | null

): Promise<RagResponse> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const params = new URLSearchParams({

      question,

      limit: String(limit),

   });

   if (documentId) {

      params.set("documentId", documentId);

   }

   const response = await fetch(

      `${API_BASE_URL}/api/rag?${params.toString()}`,

      {

         method: "GET",

         headers: {

            Authorization: `Bearer ${token}`,

         },

      }

   );

   /*

     * 401 means the JWT is invalid or expired.

     * Clear the browser session and redirect to login.

     */

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   const responseText = await response.text();

   let data:

      | RagResponse

      | { error?: string; message?: string };

   try {

      data = JSON.parse(responseText);

   } catch {

      throw new Error(

         responseText || "Invalid response from RAG API"

      );

   }

   if (!response.ok) {

      throw new Error(

         ("error" in data && data.error) ||

            ("message" in data && data.message) ||

            "Failed to get an answer"

      );

   }

   return data as RagResponse;

}

/* ---------------- STREAMING RAG ---------------- */

export async function streamRag(

   question: string,

   limit = 8,

   onChunk: (chunk: string) => void,

   onSources: (sources: RagSource[]) => void,

   documentId?: string | null

): Promise<void> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const params = new URLSearchParams({

      question,

      limit: String(limit),

   });

   if (documentId) {

      params.set("documentId", documentId);

   }

   const response = await fetch(

      `${API_BASE_URL}/api/rag/stream?${params.toString()}`,

      {

         method: "GET",

         headers: {

            Authorization: `Bearer ${token}`,

            Accept: "text/event-stream",

         },

      }

   );

   /*

     * Streaming requests also need the same 401 handling.

     */

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   if (!response.ok) {

      const text = await response.text();

      let message = text;

      try {

         const data = JSON.parse(text);

         message =

            data.error ||

            data.message ||

            text;

      } catch {

         // Response was not JSON.

      }

      throw new Error(

         message || "Failed to stream answer"

      );

   }

   if (!response.body) {

      throw new Error(

         "Streaming is not supported by this response"

      );

   }

   const reader =

      response.body.getReader();

   const decoder =

      new TextDecoder();

   let buffer = "";

   try {

      while (true) {

         const { value, done } =

            await reader.read();

         if (done) {

            break;

         }

         buffer += decoder.decode(

            value,

            {

               stream: true,

            }

         );

         const events =

            buffer.split(/\r?\n\r?\n/);

         buffer =

            events.pop() ?? "";

         for (const event of events) {

            const lines =

               event.split(/\r?\n/);

            let eventName = "";

            const dataLines: string[] = [];

            for (const line of lines) {

               if (line.startsWith("event:")) {

                  eventName =

                     line

                        .slice("event:".length)

                        .trim();

               } else if (

                  line.startsWith("data:")

               ) {

                  dataLines.push(

                     line.slice("data:".length)

                  );

               }

            }

            if (dataLines.length === 0) {

               continue;

            }

            const data =

               dataLines.join("\n");

            if (eventName === "answer") {

               onChunk(data);

            }

            if (eventName === "sources") {

               try {

                  const sources =

                     JSON.parse(data) as RagSource[];

                  onSources(sources);

               } catch (error) {

                  console.error(

                     "Failed to parse RAG sources:",

                     error

                  );

               }

            }

         }

      }

      /*

        * Process final buffered event.

        */

      buffer += decoder.decode();

      if (buffer.trim()) {

         const lines =

            buffer.split(/\r?\n/);

         let eventName = "";

         const dataLines: string[] = [];

         for (const line of lines) {

            if (line.startsWith("event:")) {

               eventName =

                  line

                     .slice("event:".length)

                     .trim();

            } else if (

               line.startsWith("data:")

            ) {

               dataLines.push(

                  line.slice("data:".length)

               );

            }

         }

         if (dataLines.length > 0) {

            const data =

               dataLines.join("\n");

            if (eventName === "answer") {

               onChunk(data);

            }

            if (eventName === "sources") {

               try {

                  const sources =

                     JSON.parse(data) as RagSource[];

                  onSources(sources);

               } catch (error) {

                  console.error(

                     "Failed to parse final RAG sources:",

                     error

                  );

               }

            }

         }

      }

   } finally {

      reader.releaseLock();

   }

}

/* ---------------- DOCUMENTS ---------------- */

export interface DocumentResponse {

   id: string;

   name: string;

   fileType: string;

   fileSize: number;

   storagePath: string;

   status: DocStatus;

   createdAt: string;

}

function normalizeDocumentStatus(

   status: string

): DocStatus {

   switch (status.toUpperCase()) {

      case "READY":

         return "Ready";

      case "PROCESSING":

         return "Processing";

      case "UPLOADING":

         return "Uploading";

      case "FAILED":

         return "Failed";

      case "UPLOADED":

         return "Ready";

      default:

         return "Processing";

   }

}

/* ---------------- UPLOAD DOCUMENT ---------------- */

export async function uploadDocument(

   file: File

): Promise<DocumentResponse> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const formData = new FormData();

   formData.append("file", file);

   const response = await fetch(

      `${API_BASE_URL}/api/documents`,

      {

         method: "POST",

         headers: {

            Authorization: `Bearer ${token}`,

         },

         body: formData,

      }

   );

   /*

     * Handle expired/invalid JWT before processing

     * the upload response.

     */

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   const responseText =

      await response.text();

   if (!response.ok) {

      let message = responseText;

      try {

         const data =

            JSON.parse(responseText);

         message =

            data.error ||

            data.message ||

            message;

      } catch {

         // Response was not JSON.

      }

      throw new Error(

         message ||

            "Failed to upload document"

      );

   }

   if (!responseText.trim()) {

      return {

         id: "",

         name: file.name,

         fileType: file.type,

         fileSize: file.size,

         storagePath: "",

         status: "Uploading",

         createdAt:

            new Date().toISOString(),

      };

   }

   try {

      const data =

         JSON.parse(responseText);

      return {

         ...data,

         status: normalizeDocumentStatus(

            data.status

         ),

      };

   } catch {

      throw new Error(

         "Document uploaded, but the server returned an invalid response."

      );

   }

}

/* ---------------- DOCUMENT FILE ---------------- */

export async function getDocumentFile(

   documentId: string

): Promise<Blob> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const response = await fetch(

      `${API_BASE_URL}/api/documents/${documentId}/file`,

      {

         method: "GET",

         headers: {

            Authorization: `Bearer ${token}`,

         },

      }

   );

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   if (!response.ok) {

      const text =

         await response.text();

      throw new Error(

         text ||

            "Failed to load document"

      );

   }

   return response.blob();

}

/* ---------------- GET DOCUMENTS ---------------- */

export async function getDocuments(): Promise<

   DocumentResponse[]

> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
      notifyUnauthorized();
      throw new Error("You are not authenticated");
    }

   const response = await fetch(

      `${API_BASE_URL}/api/documents`,

      {

         method: "GET",

         headers: {

            Authorization: `Bearer ${token}`,

         },

      }

   );

   /*

     * This is the specific request that previously

     * returned 401 with your invalid JWT.

     */

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   if (!response.ok) {

      const text =

         await response.text();

      throw new Error(

         text ||

            "Failed to fetch documents"

      );

   }

   const data =

      await response.json();

   return data.map(

      (document: {

         id: string;

         name: string;

         fileType: string;

         fileSize: number;

         storagePath: string;

         status: string;

         createdAt: string;

      }) => ({

         ...document,

         status:

            normalizeDocumentStatus(

               document.status

            ),

      })

   );

}

/* ---------------- DELETE DOCUMENT ---------------- */

export async function deleteDocument(

   documentId: string

): Promise<void> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const response = await fetch(

      `${API_BASE_URL}/api/documents/${documentId}`,

      {

         method: "DELETE",

         headers: {

            Authorization: `Bearer ${token}`,

         },

      }

   );

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   if (!response.ok) {

      const text =

         await response.text();

      throw new Error(

         text ||

            "Failed to remove document"

      );

   }

}

/* ---------------- AUDIT LOGS ---------------- */

export interface AuditLogResponse {

   id: string;

   userId: string | null;

   username: string | null;

   action: string;

   resourceType: string | null;

   resourceId: string | null;

   createdAt: string;

}

export interface AuditLogPage {

   content: AuditLogResponse[];

   page: number;

   size: number;

   totalElements: number;

   totalPages: number;

   first: boolean;

   last: boolean;

}

export async function getAuditLogs(

   page = 0,

   size = 20

): Promise<AuditLogPage> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const url =

      `${API_BASE_URL}/api/audit-logs?page=${page}&size=${size}`;

   const response = await fetch(url, {

      method: "GET",

      headers: {

         Authorization: `Bearer ${token}`,

      },

      cache: "no-store",

   });

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   const responseText =

      await response.text();

   if (!response.ok) {

      let message = responseText;

      try {

         const data =

            JSON.parse(responseText);

         message =

            data.error ||

            data.message ||

            message;

      } catch {

         // Response was not JSON.

      }

      throw new Error(

         message ||

            "Failed to fetch audit logs"

      );

   }

   try {

      return JSON.parse(responseText);

   } catch {

      throw new Error(

         "Audit API returned an invalid response."

      );

   }

}

/* ---------------- ADMIN DASHBOARD ---------------- */

export interface AdminDashboardResponse {

   totalUsers: number;

   totalAdmins: number;

   totalDocuments: number;

   totalAuditLogs: number;

}

export async function getAdminDashboard(): Promise<AdminDashboardResponse> {

   const token =

      sessionStorage.getItem("vaultrag_token");

   if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

   const response = await fetch(

      `${API_BASE_URL}/api/admin/dashboard`,

      {

         method: "GET",

         headers: {

            Authorization: `Bearer ${token}`,

         },

         cache: "no-store",

      }

   );

   if (handleUnauthorized(response)) {

     throw new Error("You are not authenticated");

   }

   const responseText = await response.text();

   if (!response.ok) {

      let message = responseText;

      try {

         const data = JSON.parse(responseText);

         message =

            data.error ||

            data.message ||

            message;

      } catch {

         // Response was not JSON.

      }

      throw new Error(

         message || "Failed to fetch admin dashboard"

      );

   }

   try {

      return JSON.parse(responseText) as AdminDashboardResponse;

   } catch {

      throw new Error(

         "Admin dashboard API returned an invalid response."

      );

   }

}

/* ---------------- ADMIN USERS ---------------- */

export interface AdminUserSummary {

  id: string;

  username: string;

  role: string;

  createdAt: string;

}

export interface AdminUsersPage {

  content: AdminUserSummary[];

  page: number;

  size: number;

  totalElements: number;

  totalPages: number;

  first: boolean;

  last: boolean;

}

/* ---------------- ADMIN USER AUDIT LOGS ---------------- */

export interface AuditLogEntry {

  id: string;

  time: string;

  action: string;

  resource: string;

}

export interface AuditLogEntry {

  id: string;

  time: string;

  action: string;

  resource: string;

}

export async function getAdminUsers(

  page = 0,

  size = 20

): Promise<AdminUsersPage> {

  const token =

    sessionStorage.getItem("vaultrag_token");

  if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

  const response = await fetch(

    `${API_BASE_URL}/api/admin/users?page=${page}&size=${size}`,

    {

      method: "GET",

      headers: {

        Authorization: `Bearer ${token}`,

      },

      cache: "no-store",

    }

  );

  if (handleUnauthorized(response)) {

    throw new Error("You are not authenticated");

  }

  const responseText = await response.text();

  if (!response.ok) {

    let message = responseText;

    try {

      const data = JSON.parse(responseText);

      message =

        data.error ||

        data.message ||

        message;

    } catch {

      // Keep response text.

    }

    throw new Error(

      message || "Failed to fetch admin users"

    );

  }

  try {

    const data = JSON.parse(responseText);

    return {

      content: data.content,

      page: data.page,

      size: data.size,

      totalElements: data.totalElements,

      totalPages: data.totalPages,

      first: data.first,

      last: data.last,

    };

  } catch {

    throw new Error(

      "Admin users API returned an invalid response."

    );

  }

}

export async function getUserAuditLogs(

  userId: string,

  page = 0,

  size = 20

): Promise<AuditLogEntry[]> {

  const token =

    sessionStorage.getItem("vaultrag_token");

  if (!token) {
    notifyUnauthorized();
    throw new Error("You are not authenticated");
  }

  const response = await fetch(

    `${API_BASE_URL}/api/admin/users/${userId}/audit-logs?page=${page}&size=${size}`,

    {

      method: "GET",

      headers: {

        Authorization: `Bearer ${token}`,

      },

      cache: "no-store",

    }

  );

  if (handleUnauthorized(response)) {

    throw new Error("You are not authenticated");

  }

  const responseText = await response.text();

  if (!response.ok) {

    let message = responseText;

    try {

      const data = JSON.parse(responseText);

      message =

        data.error ||

        data.message ||

        message;

    } catch {

      // Keep response text.

    }

    throw new Error(

      message || "Failed to fetch user audit logs"

    );

  }

  try {

    const data = JSON.parse(responseText);

    return data.content.map(

      (log: {

        id: string;

        userId: string | null;

        username: string | null;

        action: string;

        resourceType: string | null;

        resourceId: string | null;

        createdAt: string;

      }) => ({

        id: log.id,

        time: new Date(

          log.createdAt

        ).toLocaleTimeString([], {

          hour: "2-digit",

          minute: "2-digit",

        }),

        action: log.action,

        resource:

          log.resourceType ??

          log.resourceId ??

          "—",

      })

    );

  } catch {

    throw new Error(

      "User audit API returned an invalid response."

    );

  }

}
