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
      <div className="container mx-auto p-4">
        <Card>
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
    <div className="container mx-auto p-4">
      <Card>
        <CardHeader>
          <CardTitle>Account Information</CardTitle>
          <CardDescription>Your account details</CardDescription>
        </CardHeader>
        <CardContent className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-700">Email</label>
            <p className="mt-1 text-sm text-gray-900">{accountInfo?.email || user.email}</p>
          </div>
          <div>
            <label className="block text-sm font-medium text-gray-700">Role</label>
            <p className="mt-1 text-sm text-gray-900">{accountInfo?.role || "No role assigned"}</p>
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