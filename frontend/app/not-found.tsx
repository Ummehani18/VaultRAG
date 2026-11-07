import Link from "next/link";
import { FileQuestion, ArrowLeft } from "lucide-react";
import { Button } from "@/components/ui/Button";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-vault-bg text-vault-text p-8">
      <div className="w-full max-w-md text-center">
        <div className="w-14 h-14 mx-auto mb-6 rounded-full border-2 border-vault-border bg-vault-accent flex items-center justify-center">
          <FileQuestion size={24} strokeWidth={2} />
        </div>

        <p className="text-xs font-mono tracking-wide text-vault-muted mb-3">ERROR 404</p>
        <h1 className="font-serif text-3xl leading-snug mb-3">This page isn&apos;t in the vault.</h1>
        <p className="text-sm text-vault-muted mb-8 max-w-sm mx-auto">
          The page you&apos;re looking for doesn&apos;t exist, may have moved, or you may not have
          access to it within your workspace.
        </p>

        <div className="flex items-center justify-center gap-3">
          <Link href="/ask">
            <Button variant="brutal" icon={<ArrowLeft size={15} />}>
              Back to Ask VaultRAG
            </Button>
          </Link>
          <Link href="/documents">
            <Button variant="swiss">Go to Documents</Button>
          </Link>
        </div>
      </div>
    </div>
  );
}