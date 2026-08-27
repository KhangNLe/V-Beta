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

function ReportsQueueLanding() {
  const searchParams = useSearchParams();
  const { ready } = useRequireAuth({
    redirectMode: "push",
    requireEmailVerified: true,
  });
  const reportId = searchParams.get("reportId");

  if (!ready) {
    return <PageLoader message="Loading reports…" />;
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <Card className="border border-border">
        <CardHeader>
          <CardTitle>Reports</CardTitle>
          <CardDescription>
            Admin report queue and reporter outcome context.
          </CardDescription>
        </CardHeader>
        <CardContent>
          <p className="text-sm text-muted-foreground">
            {reportId
              ? `Opened report ${reportId}. The ranked queue UI is not in this slice.`
              : "The ranked report queue UI is not in this slice."}
          </p>
        </CardContent>
      </Card>
    </div>
  );
}

export default function ReportsPage() {
  return (
    <Suspense fallback={<PageLoader message="Loading reports…" />}>
      <ReportsQueueLanding />
    </Suspense>
  );
}
