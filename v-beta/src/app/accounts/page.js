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
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { ChevronDown } from "lucide-react";
import PageLoader from "@/components/ui/PageLoader";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useEffect, useState } from "react";
import { toast } from "react-toastify";

const ROLES = ["climber", "setter", "admin"];

const updateAccountRole = async (accountId, newRole) => {
  //TODO: Implement API call to update account role.
  toast.info(`Role updating not quite implemented yet :) ${newRole}`);
};

export default function AccountsPage() {
  const { user, loading: authLoading } = useRequireAuth();
  const [accounts, setAccounts] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [savingRoles, setSavingRoles] = useState({});

  useEffect(() => {
    if (user && !authLoading) {
      loadAccounts();
    }
  }, [user, authLoading]);

  const loadAccounts = async () => {
    try {
      setLoading(true);
      setError(null);
      const accountsData = await fetchAllAccounts(user);
      setAccounts(accountsData);
    } catch (err) {
      setError(err.message);
      toast.error(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleRoleSelect = async (accountId, currentRole, newRole) => {
    if (newRole === currentRole?.toLowerCase()) return;

    setSavingRoles((prev) => ({ ...prev, [accountId]: true }));
    setAccounts((prev) =>
      prev.map((account) =>
        account.id === accountId ? { ...account, roleName: newRole.toUpperCase() } : account
      )
    );

    try {
      await updateAccountRole(accountId, newRole);
    } catch (err) {
      toast.error(`Failed to update role: ${err.message}`);
      await loadAccounts();
    } finally {
      setSavingRoles((prev) => ({ ...prev, [accountId]: false }));
    }
  };

  if (authLoading || loading) {
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
          const currentRole = account.roleName?.toLowerCase();
          const isSaving = !!savingRoles[account.id];

          return (
            <Card key={account.id} className="border border-border hover:shadow-lg transition-shadow">
              <CardHeader>
                <CardTitle className="text-lg">{account.username}</CardTitle>
                <CardDescription>
                  <div className="flex items-center gap-2">
                    <span>Role:</span>
                    <DropdownMenu>
                      <DropdownMenuTrigger
                        disabled={isSaving}
                        className="flex items-center gap-1 h-6 px-2 text-xs capitalize font-medium rounded-md border border-input bg-background hover:bg-accent hover:text-accent-foreground disabled:opacity-50"
                      >
                        {currentRole ?? "Select role"}
                        <ChevronDown className="h-3 w-3" />
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="start">
                        {ROLES.map((role) => (
                          <DropdownMenuItem
                            key={role}
                            className={`text-xs capitalize cursor-pointer ${currentRole === role ? "font-semibold text-blue-600" : ""}`}
                            onClick={() => handleRoleSelect(account.id, currentRole, role)}
                          >
                            {role}
                          </DropdownMenuItem>
                        ))}
                      </DropdownMenuContent>
                    </DropdownMenu>
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
                  <p className="mt-1 text-sm font-mono">{account.id}</p>
                </div>
                <div>
                  <label className="block text-sm font-medium text-muted-foreground">Firebase UID</label>
                  <p className="mt-1 text-sm font-mono break-all">{account.firebaseUid}</p>
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
    </div>
  );
}