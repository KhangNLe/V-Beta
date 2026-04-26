import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AccountPage from "./page";
import { deleteAccount, fetchAccountInfo } from "@/api/account";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { toast } from "react-toastify";

jest.mock("@/api/account", () => ({
  fetchAccountInfo: jest.fn(),
  deleteAccount: jest.fn(),
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    error: jest.fn(),
    info: jest.fn(),
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
  CardDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  CardContent: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

jest.mock("@/components/ui/alert-dialog", () => {
  const React = require("react");
  const DialogContext = React.createContext({ onOpenChange: () => {} });

  return {
    AlertDialog: ({ children, open, onOpenChange }) => (
      <DialogContext.Provider value={{ onOpenChange }}>
        {open ? <div data-testid="alert-dialog">{children}</div> : null}
      </DialogContext.Provider>
    ),
    AlertDialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
    AlertDialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
    AlertDialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
    AlertDialogCancel: ({ children, onClick, ...props }) => {
      const { onOpenChange } = React.useContext(DialogContext);
      return (
        <button
          {...props}
          onClick={(event) => {
            onClick?.(event);
            onOpenChange(false);
          }}
        >
          {children}
        </button>
      );
    },
    AlertDialogAction: ({ children, ...props }) => <button {...props}>{children}</button>,
  };
});

describe("AccountPage", () => {
  const mockUser = {
    email: "climber@example.com",
    username: "climber1",
  };

  beforeEach(() => {
    jest.clearAllMocks();
    useRequireAuth.mockReturnValue({
      user: mockUser,
      ready: true,
    });
  });

  it("shows a loader while auth is not ready", () => {
    useRequireAuth.mockReturnValue({
      user: null,
      ready: false,
    });

    render(<AccountPage />);

    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
  });

  it("renders account data returned by the API", async () => {
    fetchAccountInfo.mockResolvedValue({
      email: "setter@example.com",
      username: "setter2",
      role: "SETTER",
    });

    render(<AccountPage />);

    await waitFor(() => {
      expect(fetchAccountInfo).toHaveBeenCalledWith(mockUser);
    });

    expect(await screen.findByRole("heading", { name: "Account Information" })).toBeInTheDocument();
    expect(screen.getByText("setter@example.com")).toBeInTheDocument();
    expect(screen.getByText("setter2")).toBeInTheDocument();
    expect(screen.getByText("SETTER")).toBeInTheDocument();
  });

  it("falls back to user values when API returns partial data", async () => {
    fetchAccountInfo.mockResolvedValue({});

    render(<AccountPage />);

    await waitFor(() => {
      expect(fetchAccountInfo).toHaveBeenCalledWith(mockUser);
    });

    expect(await screen.findByText("climber@example.com")).toBeInTheDocument();
    expect(screen.getByText("climber1")).toBeInTheDocument();
    expect(screen.getByText("No role assigned")).toBeInTheDocument();
  });

  it("shows an error card and toast if loading account data fails", async () => {
    fetchAccountInfo.mockRejectedValue(new Error("Forbidden"));

    render(<AccountPage />);

    expect(await screen.findByText("Error loading account information")).toBeInTheDocument();
    expect(screen.getByText("Forbidden")).toBeInTheDocument();
    expect(toast.error).toHaveBeenCalledWith("Failed to load account information");
  });

  it("opens and closes the delete confirmation dialog", async () => {
    fetchAccountInfo.mockResolvedValue({
      email: "admin@example.com",
      username: "admin1",
      role: "ADMIN",
    });

    render(<AccountPage />);

    expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
    fireEvent.click(await screen.findByRole("button", { name: "Delete Account" }));
    expect(screen.getByTestId("alert-dialog")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Cancel" }));
    expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
  });

  it("submits deletion and closes dialog on success", async () => {
    fetchAccountInfo.mockResolvedValue({
      email: "admin@example.com",
      username: "admin1",
      role: "ADMIN",
    });
    deleteAccount.mockResolvedValue(undefined);

    render(<AccountPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Delete Account" }));
    fireEvent.click(screen.getByRole("button", { name: /^Delete$/ }));

    await waitFor(() => {
      expect(deleteAccount).toHaveBeenCalledWith(mockUser);
    });
    expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
  });

  it("shows deleting state while deletion is pending", async () => {
    fetchAccountInfo.mockResolvedValue({
      email: "admin@example.com",
      username: "admin1",
      role: "ADMIN",
    });
    let resolveDelete;
    deleteAccount.mockImplementation(
      () =>
        new Promise((resolve) => {
          resolveDelete = resolve;
        }),
    );

    render(<AccountPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Delete Account" }));
    fireEvent.click(screen.getByRole("button", { name: /^Delete$/ }));

    expect(screen.getByRole("button", { name: "Deleting..." })).toBeDisabled();

    resolveDelete();
    await waitFor(() => {
      expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
    });
  });

  it("shows an error toast when deletion fails", async () => {
    fetchAccountInfo.mockResolvedValue({
      email: "admin@example.com",
      username: "admin1",
      role: "ADMIN",
    });
    deleteAccount.mockRejectedValue(new Error("Permission denied"));

    render(<AccountPage />);

    fireEvent.click(await screen.findByRole("button", { name: "Delete Account" }));
    fireEvent.click(screen.getByRole("button", { name: /^Delete$/ }));

    await waitFor(() => {
      expect(toast.error).toHaveBeenCalledWith(
        "Failed to delete account: Permission denied",
      );
    });
    expect(screen.queryByTestId("alert-dialog")).not.toBeInTheDocument();
  });
});
