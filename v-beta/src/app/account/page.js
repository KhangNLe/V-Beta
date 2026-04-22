"use client";

import { fetchAccountInfo } from "@/api/account";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

export default function AccountPage() {
  const { user, ready } = useRequireAuth({ redirectMode: "push" });
  const [accountInfo, setAccountInfo] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    if (ready && user) {
      fetchAccountInfo(user)
        .then((data) => {
          console.log("Account info received:", data);
          setAccountInfo(data);
        })
        .catch((err) => {
          setError(err.message);
          toast.error("Failed to load account information");
        })
        .finally(() => setLoading(false));
    }
  }, [user, ready]);

  if (!ready || loading) {
    return <PageLoader />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Account</CardTitle>
            <CardDescription>Error loading account information</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-red-500">{error}</p>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <Card className="border border-border">
        <CardHeader>
          <CardTitle>Account Information</CardTitle>
          <CardDescription>Your account details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-muted-foreground">Email</label>
            <p className="mt-1 text-sm">{accountInfo?.email || user.email}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground">Username</label>
            <p className="mt-1 text-sm">{accountInfo?.username || user.username}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-muted-foreground">Role</label>
            <p className="mt-1 text-sm">{accountInfo?.role || "No role assigned"}</p>
          </div>
          <div className="pt-4">
            <Button
              variant="destructive"
              onClick={() => toast.info("Delete account functionality not implemented yet")}
            >
              Delete Account
            </Button>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}