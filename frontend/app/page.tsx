"use client";

import { useEffect, useState } from "react";

export default function Home() {
  const [status, setStatus] = useState("Checking...");

  useEffect(() => {
    fetch("/api/health")
      .then((response) => response.json())
      .then((data) => setStatus(data.status))
      .catch(() => setStatus("DOWN"));
  }, []);

  return (
    <main className="min-h-screen flex items-center justify-center">
      <div className="text-center">
        <h1 className="text-4xl font-bold">
          VaultRAG
        </h1>

        <p className="mt-4 text-lg">
          Backend Status:{" "}
          <span className="font-semibold">{status}</span>
        </p>
      </div>
    </main>
  );
}