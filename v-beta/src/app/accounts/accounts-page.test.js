import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AccountsPage from "./page";
import { fetchAllAccounts } from "@/api/accounts";
import { changeAccountRole } from "@/api/promoteOrDemote";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/accounts", () => ({
  fetchAllAccounts: jest.fn(),
}));

jest.mock("@/api/promoteOrDemote", () => ({
  changeAccountRole: jest.fn(),
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: () => <div data-testid="page-loader">Loading...</div>,
}));

jest.mock("@/components/ui/button", () => ({
  Button: ({ children, ...props }) => <button {...props}>{children}</button>,
}));

jest.mock("@/components/ui/card", () => ({
  Card: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardTitle: ({ children, ...props }) => <h2 {...props}>{children}</h2>,
  CardDescription: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardContent: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

jest.mock("@/components/ui/dialog", () => ({
  Dialog: ({ children, open }) => (open ? <div data-testid="role-dialog">{children}</div> : null),
  DialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
  DialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  DialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

const mockReplace = jest.fn();
const mockUser = { uid: "u1", email: "admin@example.com" };
const adminSession = { id: 1, roleName: "ADMIN" };
const climberSession = { id: 1, roleName: "CLIMBER" };

const oneAccount = [
  {
    id: 1,
    username: "admin",
    email: "admin@example.com",
    firebaseUid: "firebase-1",
    roleName: "ADMIN",
  },
];

const manyAccounts = [
  {
    id: 1,
    username: "admin",
    email: "admin@example.com",
    firebaseUid: "firebase-1",
    roleName: "ADMIN",
  },
  {
    id: 2,
    username: "setter",
    email: "setter@example.com",
    firebaseUid: "firebase-2",
    roleName: "SETTER",
  },
];

function renderAccounts({
  ready = true,
  user = mockUser,
  account = adminSession,
  accounts = oneAccount,
} = {}) {
  useRequireAuth.mockReturnValue({ ready, user, account });
  fetchAllAccounts.mockResolvedValue(accounts);
  return render(<AccountsPage />);
}

describe("AccountsPage coverage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ replace: mockReplace });
  });

  it("shows loader while auth is not ready", () => {
    renderAccounts({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchAllAccounts).not.toHaveBeenCalled();
  });

  it("redirects non-admin users to main page", async () => {
    renderAccounts({ account: climberSession, accounts: [] });
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
    expect(fetchAllAccounts).not.toHaveBeenCalled();
  });

  it("shows empty state when there are zero accounts", async () => {
    renderAccounts({ accounts: [] });
    expect(await screen.findByText("No accounts found.")).toBeInTheDocument();
  });

  it("renders one account and opens role dialog with selected role", async () => {
    renderAccounts({ accounts: oneAccount });
    expect(await screen.findByText("admin")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Change Role" }));
    const select = screen.getByLabelText("New role");
    expect(select).toHaveValue("ADMIN");
    expect(screen.getByRole("button", { name: "Submit" })).toBeDisabled();
  });

  it("renders many accounts and keeps role updates scoped to one account", async () => {
    changeAccountRole.mockResolvedValue(undefined);
    renderAccounts({ accounts: manyAccounts });

    expect(await screen.findByText("admin")).toBeInTheDocument();
    expect(screen.getByText("setter")).toBeInTheDocument();

    const cards = screen.getAllByRole("button", { name: "Change Role" });
    fireEvent.click(cards[1]);
    fireEvent.change(screen.getByLabelText("New role"), { target: { value: "ADMIN" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(changeAccountRole).toHaveBeenCalledWith(mockUser, 2, "ADMIN");
    });
    expect(screen.getAllByText("ADMIN").length).toBeGreaterThan(0);
  });

  it("defaults role selector to CLIMBER when role is missing", async () => {
    renderAccounts({
      accounts: [{ ...oneAccount[0], roleName: null }],
    });
    fireEvent.click(await screen.findByRole("button", { name: "Change Role" }));
    expect(screen.getByLabelText("New role")).toHaveValue("CLIMBER");
  });

  it("refresh button re-fetches accounts", async () => {
    renderAccounts({ accounts: oneAccount });
    await screen.findByText("admin");

    fireEvent.click(screen.getByRole("button", { name: "Refresh Accounts" }));

    await waitFor(() => {
      expect(fetchAllAccounts).toHaveBeenCalledTimes(2);
    });
  });

  it("closes dialog and resets selected role on cancel", async () => {
    renderAccounts({ accounts: oneAccount });
    fireEvent.click(await screen.findByRole("button", { name: "Change Role" }));
    fireEvent.change(screen.getByLabelText("New role"), { target: { value: "SETTER" } });
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));

    expect(screen.queryByTestId("role-dialog")).not.toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "Change Role" }));
    expect(screen.getByLabelText("New role")).toHaveValue("ADMIN");
  });

  it("shows error card and retry for non-access-denied fetch failures", async () => {
    fetchAllAccounts.mockRejectedValue(new Error("Failed to fetch accounts: 500"));
    useRequireAuth.mockReturnValue({ ready: true, user: mockUser, account: adminSession });
    render(<AccountsPage />);

    expect(await screen.findByText("Error loading accounts")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Try Again" }));
    await waitFor(() => {
      expect(fetchAllAccounts).toHaveBeenCalledTimes(2);
    });
  });

  it("redirects to main page when accounts fetch throws access denied", async () => {
    fetchAllAccounts.mockRejectedValue(new Error("Access denied."));
    useRequireAuth.mockReturnValue({ ready: true, user: mockUser, account: adminSession });
    render(<AccountsPage />);

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
  });

  it("shows per-account saving UI and disables submit while role change is pending", async () => {
    let resolveChange;
    changeAccountRole.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveChange = resolve;
        }),
    );
    renderAccounts({ accounts: oneAccount });
    fireEvent.click(await screen.findByRole("button", { name: "Change Role" }));
    fireEvent.change(screen.getByLabelText("New role"), { target: { value: "SETTER" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    expect(screen.getByText("Saving...")).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Submitting..." })).toBeDisabled();
    expect(screen.getByLabelText("New role")).toBeDisabled();

    resolveChange(undefined);
    await waitFor(() => {
      expect(screen.queryByText("Saving...")).not.toBeInTheDocument();
    });
  });

  it("redirects when current admin self-demotes", async () => {
    changeAccountRole.mockResolvedValue(undefined);
    renderAccounts({ account: { id: 1, roleName: "ADMIN" }, accounts: oneAccount });
    fireEvent.click(await screen.findByRole("button", { name: "Change Role" }));
    fireEvent.change(screen.getByLabelText("New role"), { target: { value: "SETTER" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
  });

  it("does not redirect when self-demotion role change fails", async () => {
    changeAccountRole.mockRejectedValue(new Error("Failed to update account role: 500"));
    renderAccounts({ account: { id: 1, roleName: "ADMIN" }, accounts: oneAccount });
    fireEvent.click(await screen.findByRole("button", { name: "Change Role" }));
    fireEvent.change(screen.getByLabelText("New role"), { target: { value: "SETTER" } });
    fireEvent.click(screen.getByRole("button", { name: "Submit" }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "Failed to update account role: Failed to update account role: 500",
      );
    });
    expect(toast.error).toHaveBeenCalledTimes(1);
    expect(mockReplace).not.toHaveBeenCalledWith("/main-page");
    expect(fetchAllAccounts).toHaveBeenCalledTimes(2);
  });
});
