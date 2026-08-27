"use client";

import PageLoader from "@/components/ui/PageLoader";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useSearchParams } from "next/navigation";
import { Suspense } from "react";

function AppealContextLanding() {
  const searchParams = useSearchParams();
  const { ready } = useRequireAuth({
    redirectMode: "push",
    requireEmailVerified: true,
  });
  const reportId = searchParams.get("reportId");

  if (!ready) {
    return <PageLoader message="Loading appeal…" />;
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <Card className="border border-border">
        <CardHeader>
          <CardTitle>Appeals</CardTitle>
          <CardDescription>
            Deletion and appeal context for reported content.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            {reportId
              ? `Opened appeal context for report ${reportId}. Appeal submit/review UI is not in this slice.`
              : "Appeal submit/review UI is not in this slice."}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default function AppealsPage() {
  return (
    <Suspense fallback={<PageLoader message="Loading appeal…" />}>
      <AppealContextLanding />
    </Suspense>
  );
}
