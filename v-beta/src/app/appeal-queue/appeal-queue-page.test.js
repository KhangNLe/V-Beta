import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import AppealQueuePage from "./page";
import { fetchAppeals, resolveAppeal } from "@/api/appeals";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/appeals", () => ({
  fetchAppeals: jest.fn(),
  resolveAppeal: jest.fn(),
  ADMIN_REASON_MAX_LENGTH: 255,
  APPEAL_RESOLVE_STATUSES: { APPROVED: "APPROVED", DENIED: "DENIED" },
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    success: jest.fn(),
    error: jest.fn(),
  },
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="page-loader">{message || "Loading..."}</div>,
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

jest.mock("@/components/ui/dialog", () => ({
  Dialog: ({ children, open }) =>
    open ? <div data-testid="appeal-dialog">{children}</div> : null,
  DialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
  DialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  DialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

const mockReplace = jest.fn();
const user = { uid: "firebase-uid", email: "admin@example.com" };
const adminSession = { id: 1, roleName: "ADMIN" };

const openAppeal = {
  appealId: 7,
  appealUser: { userId: 8, username: "alex" },
  appealReason: "This was a joke, please restore.",
  report: {
    discussion: {
      discussionType: "COMMENT",
      discussionContent: "hello",
    },
    reporters: [
      {
        reportId: 11,
        reporter: { username: "sam" },
        categoryName: "SPAM",
        reportReason: "Spammy",
      },
    ],
  },
};

function renderQueue({
  ready = true,
  currentUser = user,
  account = adminSession,
  reportId = "",
  appeals = [openAppeal],
  setupFetch = true,
} = {}) {
  useRequireAuth.mockReturnValue({ ready, user: currentUser, account });
  useSearchParams.mockReturnValue({ get: () => reportId || null });
  if (setupFetch) {
    fetchAppeals.mockResolvedValue({ appeals });
  }
  return render(<AppealQueuePage />);
}

describe("AppealQueuePage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ replace: mockReplace });
    useSearchParams.mockReturnValue({ get: () => null });
  });

  it("shows loader while auth is not ready", () => {
    renderQueue({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchAppeals).not.toHaveBeenCalled();
  });

  it("redirects non-admins", async () => {
    renderQueue({ account: { id: 2, roleName: "CLIMBER" } });
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
    expect(fetchAppeals).not.toHaveBeenCalled();
  });

  it("lists open appeals from AppealDTO", async () => {
    renderQueue();
    expect(await screen.findByText("alex")).toBeInTheDocument();
    expect(screen.getByText("This was a joke, please restore.")).toBeInTheDocument();
  });

  it("opens appeal detail with reporter identity", async () => {
    renderQueue();
    fireEvent.click(await screen.findByText("alex"));
    expect(await screen.findByTestId("appeal-dialog")).toBeInTheDocument();
    expect(screen.getByText("User appeal")).toBeInTheDocument();
    expect(screen.getByText("hello")).toBeInTheDocument();
    expect(screen.getByText(/sam/)).toBeInTheDocument();
    expect(screen.getByText("Spammy")).toBeInTheDocument();
    expect(mockReplace).toHaveBeenCalledWith("/appeal-queue?reportId=11");
  });

  it("loads a deep-linked report from GET /api/moderate/appeal?reportId=", async () => {
    fetchAppeals
      .mockResolvedValueOnce({ appeals: [] })
      .mockResolvedValueOnce({ appeals: [openAppeal] });
    renderQueue({ reportId: "11", setupFetch: false });
    expect(await screen.findByTestId("appeal-dialog")).toBeInTheDocument();
    expect(screen.getByText("This was a joke, please restore.")).toBeInTheDocument();
    expect(fetchAppeals).toHaveBeenCalledWith(user, { reportId: 11 });
  });

  it("disables approve and deny without admin comments", async () => {
    renderQueue();
    fireEvent.click(await screen.findByText("alex"));
    expect(await screen.findByRole("button", { name: "Approve" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Deny" })).toBeDisabled();
  });

  it("approves an appeal with comments and refreshes the queue", async () => {
    resolveAppeal.mockResolvedValue(undefined);
    renderQueue();
    fireEvent.click(await screen.findByText("alex"));
    fireEvent.change(await screen.findByLabelText("Admin comments"), {
      target: { value: "Restored after review." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Approve" }));
    await waitFor(() => {
      expect(resolveAppeal).toHaveBeenCalledWith(user, {
        appealId: 7,
        appealStatus: "APPROVED",
        adminReason: "Restored after review.",
      });
    });
    expect(fetchAppeals).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith("Appeal approved. Content restored.");
  });

  it("denies an appeal with comments", async () => {
    resolveAppeal.mockResolvedValue(undefined);
    renderQueue();
    fireEvent.click(await screen.findByText("alex"));
    fireEvent.change(await screen.findByLabelText("Admin comments"), {
      target: { value: "Removal stands." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Deny" }));
    await waitFor(() => {
      expect(resolveAppeal).toHaveBeenCalledWith(user, {
        appealId: 7,
        appealStatus: "DENIED",
        adminReason: "Removal stands.",
      });
    });
    expect(toast.success).toHaveBeenCalledWith("Appeal denied.");
  });
});
