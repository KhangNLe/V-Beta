"use client";

import { fetchAllAccounts } from "@/api/accounts";
import { Button } from "@/components/ui/button";
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from "@/components/ui/card";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { getAccountId, getAccountRole } from "@/lib/accountSession";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";
import { changeAccountRole } from "@/api/promoteOrDemote";
import { useRouter } from "next/navigation";

const ROLES = ["CLIMBER", "SETTER", "ADMIN"];

const updateAccountRole = async (user, accountId, newRole) => {
  try {
    await changeAccountRole(user, accountId, newRole);
    toast.success("Account role updated successfully.");
  } catch (err) {
    toast.error(`Failed to update account role: ${err.message}`);
    throw err;
  }
};

export default function AccountsPage() {
  const router = useRouter();
  const { user, account, ready } = useRequireAuth({ requireEmailVerified: true });
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [savingRoles, setSavingRoles] = useState({});
  const [roleDialogOpen, setRoleDialogOpen] = useState(false);
  const [selectedAccount, setSelectedAccount] = useState(null);
  const [selectedRole, setSelectedRole] = useState("");

  const isAdmin = getAccountRole(account).toUpperCase().includes("ADMIN");

  useEffect(() => {
    if (!ready) return;
    if (!user) return;

    if (!isAdmin) {
      router.replace("/main-page");
      return;
    }

    loadAccounts();
  }, [ready, user, isAdmin, router]);
  const loadAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const accountsData = await fetchAllAccounts(user);
      setAccounts(accountsData);
    } catch (err) {
      if (err?.message === "Access denied.") {
        router.replace("/main-page");
        return;
      }
      setError(err.message);
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRoleSelect = async (accountId, currentRole, newRole) => {
    if (newRole === currentRole?.toUpperCase()) return;

    setSavingRoles((prev) => ({ ...prev, [accountId]: true }));
    setAccounts((prev) =>
      prev.map((listedAccount) =>
        getAccountId(listedAccount) === accountId
          ? { ...listedAccount, role: newRole.toUpperCase(), roleName: newRole.toUpperCase() }
          : listedAccount
      )
    );

    try {
      await updateAccountRole(user, accountId, newRole);
      if (getAccountId(account) === accountId && newRole.toUpperCase() !== "ADMIN") {
        router.replace("/main-page");
        return;
      }
    } catch (err) {
      await loadAccounts();
    } finally {
      setSavingRoles((prev) => ({ ...prev, [accountId]: false }));
    }
  };

  const openRoleDialog = (account) => {
    setSelectedAccount(account);
    setSelectedRole(getAccountRole(account).toUpperCase() || ROLES[0]);
    setRoleDialogOpen(true);
  };

  const closeRoleDialog = () => {
    setRoleDialogOpen(false);
    setSelectedAccount(null);
    setSelectedRole("");
  };

  const handleRoleSubmit = async () => {
    if (!selectedAccount || !selectedRole) return;
    const currentRole = getAccountRole(selectedAccount).toUpperCase();
    await handleRoleSelect(getAccountId(selectedAccount), currentRole, selectedRole.toUpperCase());
    closeRoleDialog();
  };

  if (!ready || loading) {
    return <PageLoader />;
  }

  if (error) {
    return (
      <div className="container mx-auto min-h-screen p-4">
        <Card className="border border-border">
          <CardHeader>
            <CardTitle>Accounts</CardTitle>
            <CardDescription>Error loading accounts</CardDescription>
          </CardHeader>
          <CardContent>
            <p className="text-red-500">{error}</p>
            <Button onClick={loadAccounts} className="mt-4">Try Again</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  return (
    <div className="container mx-auto min-h-screen p-4">
      <div className="mb-6">
        <h1 className="text-3xl font-bold mb-1">All Accounts</h1>
        <p className="text-sm text-muted-foreground">View and manage all user accounts in the system.</p>
      </div>

      <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-3">
        {accounts.map((account) => {
          const currentRole = getAccountRole(account).toUpperCase();
          const accountId = getAccountId(account);
          const isSaving = !!savingRoles[accountId];

          return (
            <Card key={accountId} className="border border-border hover:shadow-lg transition-shadow">
              <CardHeader>
                <CardTitle className="text-lg">{account.username}</CardTitle>
                <CardDescription>
                  <div className="flex items-center gap-2">
                    <span>Role:</span>
                    <span className="text-xs capitalize font-medium text-muted-foreground">
                      {currentRole ?? "No role"}
                    </span>
                    <Button
                      type="button"
                      variant="outline"
                      className="h-6 px-2 text-xs"
                      disabled={isSaving}
                      onClick={() => openRoleDialog(account)}
                    >
                      Change Role
                    </Button>
                    {isSaving && (
                      <span className="text-xs animate-pulse">Saving...</span>
                    )}
                  </div>
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-muted-foreground">Email</label>
                  <p className="mt-1 text-sm break-all">{account.email}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-muted-foreground">User ID</label>
                  <p className="mt-1 text-sm font-mono">{accountId}</p>
                </div>
              </CardContent>
            </Card>
          );
        })}
      </div>

      {accounts.length === 0 && (
        <div className="text-center py-12">
          <p className="text-muted-foreground text-lg">No accounts found.</p>
        </div>
      )}

      <div className="mt-6">
        <Button onClick={loadAccounts} variant="outline">
          Refresh Accounts
        </Button>
      </div>

      <Dialog
        open={roleDialogOpen}
        onOpenChange={(open) => {
          if (!open) {
            closeRoleDialog();
          } else {
            setRoleDialogOpen(true);
          }
        }}
      >
        <DialogContent className="sm:max-w-md">
          <DialogHeader>
            <DialogTitle>Confirm Role Change</DialogTitle>
            <DialogDescription>
              Select a new role for {selectedAccount?.username || "this account"} and submit to apply.
            </DialogDescription>
          </DialogHeader>

          <div className="grid gap-2">
            <label htmlFor="role-select" className="text-sm font-medium">
              New role
            </label>
            <select
              id="role-select"
              className="h-9 rounded-md border border-input bg-background px-3 text-sm"
              value={selectedRole}
              onChange={(event) => setSelectedRole(event.target.value)}
              disabled={selectedAccount ? !!savingRoles[getAccountId(selectedAccount)] : false}
            >
              {ROLES.map((role) => (
                <option key={role} value={role}>
                  {role.toUpperCase()}
                </option>
              ))}
            </select>
          </div>

          <DialogFooter className="mt-2 gap-2 sm:justify-end">
            <Button type="button" variant="outline" onClick={closeRoleDialog}>
              Cancel
            </Button>
            <Button
              type="button"
              onClick={handleRoleSubmit}
              disabled={
                !selectedAccount ||
                (getAccountRole(selectedAccount).toUpperCase() || "") === selectedRole ||
                !!savingRoles[getAccountId(selectedAccount)]
              }
            >
              {selectedAccount && savingRoles[getAccountId(selectedAccount)] ? "Submitting..." : "Submit"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}