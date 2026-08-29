import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import ReportsPage from "./page";
import { fetchReportQueue, resolveReports } from "@/api/reports";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/reports", () => ({
  fetchReportQueue: jest.fn(),
  resolveReports: jest.fn(),
  ADMIN_NOTES_MAX_LENGTH: 255,
  REPORT_RESOLVE_DECISIONS: {
    DISMISS: "REPORT_DISMISSED",
    REMOVE: "CONTENT_REMOVED",
  },
  REPORT_CATEGORIES: [
    { value: "INAPPROPRIATE_CONTENT", label: "Inappropriate content" },
    { value: "HARASSMENT_BULLYING", label: "Harassment or bullying" },
    { value: "SPAM", label: "Spam" },
    { value: "OFF_TOPIC", label: "Off-topic" },
  ],
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
  useSearchParams: jest.fn(),
}));

jest.mock("next/link", () => ({
  __esModule: true,
  default: ({ href, children, ...props }) => (
    <a href={href} {...props}>
      {children}
    </a>
  ),
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
  Dialog: ({ children, open }) => (open ? <div data-testid="report-dialog">{children}</div> : null),
  DialogContent: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  DialogTitle: ({ children, ...props }) => <h3 {...props}>{children}</h3>,
  DialogDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  DialogFooter: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

const mockReplace = jest.fn();
const user = { uid: "firebase-uid", email: "admin@example.com" };
const adminSession = { id: 1, roleName: "ADMIN" };
const climberSession = { id: 2, roleName: "CLIMBER" };

const spamCase = {
  queueScore: 4,
  categories: [{ categoryName: "INAPPROPRIATE_CONTENT", reportCount: 1, categoryScore: 4 }],
  report: {
    targetType: "DISCUSSION",
    discussion: {
      discussionId: 40,
      userId: 8,
      username: "alex",
      discussionType: "COMMENT",
      discussionContent: "hello",
    },
    climbingProblem: {
      problemId: 100,
      holdColor: "Red",
      assignedGrade: "V4",
    },
    wallSection: {
      wallSectionID: 10,
      wallSectionName: "Cave",
    },
    user: null,
    reporters: [
      {
        reportId: 11,
        reporter: { userId: 2, username: "sam", email: "sam@example.com", role: "CLIMBER" },
        categoryName: "INAPPROPRIATE_CONTENT",
        reportReason: "Offensive comment",
        createdAt: "2026-08-16T15:00:00Z",
      },
    ],
  },
};

const offTopicCase = {
  queueScore: 1,
  categories: [{ categoryName: "OFF_TOPIC", reportCount: 1, categoryScore: 1 }],
  report: {
    targetType: "DISCUSSION",
    discussion: {
      discussionId: 41,
      discussionType: "COMMENT",
      discussionContent: "later",
    },
    climbingProblem: { problemId: 100, holdColor: "Red" },
    wallSection: { wallSectionID: 10, wallSectionName: "Cave" },
    reporters: [
      {
        reportId: 12,
        reporter: { userId: 3, username: "lee", role: "CLIMBER" },
        categoryName: "OFF_TOPIC",
        reportReason: "Wrong wall",
        createdAt: "2026-08-17T15:00:00Z",
      },
    ],
  },
};

function renderReports({
  ready = true,
  session = adminSession,
  currentUser = user,
  queue = [spamCase, offTopicCase],
  reportId = null,
} = {}) {
  useRequireAuth.mockReturnValue({ ready, user: currentUser, account: session });
  useSearchParams.mockReturnValue({ get: () => reportId });
  fetchReportQueue.mockResolvedValue({ reports: queue });
  return render(<ReportsPage />);
}

describe("ReportsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ replace: mockReplace });
    useSearchParams.mockReturnValue({ get: () => null });
  });

  it("shows loader while auth is not ready", () => {
    renderReports({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchReportQueue).not.toHaveBeenCalled();
  });

  it("redirects non-admin users", async () => {
    renderReports({ session: climberSession, queue: [] });
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
    expect(fetchReportQueue).not.toHaveBeenCalled();
  });

  it("renders an empty queue", async () => {
    renderReports({ queue: [] });
    expect(await screen.findByText("No open reports.")).toBeInTheDocument();
  });

  it("lists cases in API rank order", async () => {
    renderReports();
    const items = await screen.findAllByRole("button", { name: /Inappropriate content|Off-topic/i });
    expect(items[0]).toHaveTextContent("Inappropriate content");
    expect(items[0]).toHaveTextContent("sam");
    expect(items[1]).toHaveTextContent("Off-topic");
    expect(items[1]).toHaveTextContent("lee");
  });

  it("opens detail with wall, problem, and reporter reason", async () => {
    renderReports();
    fireEvent.click(await screen.findByRole("button", { name: /Inappropriate content/i }));
    expect(await screen.findByTestId("report-dialog")).toBeInTheDocument();
    expect(screen.getByText("Cave")).toBeInTheDocument();
    expect(screen.getByText(/Red/)).toBeInTheDocument();
    expect(screen.getByText("Offensive comment")).toBeInTheDocument();
    expect(screen.getByText("hello")).toBeInTheDocument();
  });

  it("disables dismiss and approve deletion without admin notes", async () => {
    renderReports();
    fireEvent.click(await screen.findByRole("button", { name: /Inappropriate content/i }));
    expect(await screen.findByRole("button", { name: "Dismiss" })).toBeDisabled();
    expect(screen.getByRole("button", { name: "Approve deletion" })).toBeDisabled();
  });

  it("dismisses a case with notes and refreshes the queue", async () => {
    resolveReports.mockResolvedValue(undefined);
    renderReports();
    fireEvent.click(await screen.findByRole("button", { name: /Inappropriate content/i }));
    fireEvent.change(await screen.findByLabelText("Admin notes"), {
      target: { value: "Does not violate gym guidelines." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Dismiss" }));
    await waitFor(() => {
      expect(resolveReports).toHaveBeenCalledWith(user, {
        reportIds: [11],
        decision: "REPORT_DISMISSED",
        reason: "Does not violate gym guidelines.",
      });
    });
    expect(fetchReportQueue).toHaveBeenCalledTimes(2);
    expect(toast.success).toHaveBeenCalledWith("Report dismissed.");
  });

  it("approves deletion with notes", async () => {
    resolveReports.mockResolvedValue(undefined);
    renderReports();
    fireEvent.click(await screen.findByRole("button", { name: /Inappropriate content/i }));
    fireEvent.change(await screen.findByLabelText("Admin notes"), {
      target: { value: "Does not belong on this wall." },
    });
    fireEvent.click(screen.getByRole("button", { name: "Approve deletion" }));
    await waitFor(() => {
      expect(resolveReports).toHaveBeenCalledWith(user, {
        reportIds: [11],
        decision: "CONTENT_REMOVED",
        reason: "Does not belong on this wall.",
      });
    });
    expect(toast.success).toHaveBeenCalledWith("Content removed.");
  });

  it("redirects when the queue fetch is access denied", async () => {
    fetchReportQueue.mockRejectedValue(new Error("Access denied."));
    useRequireAuth.mockReturnValue({ ready: true, user, account: adminSession });
    useSearchParams.mockReturnValue({ get: () => null });
    render(<ReportsPage />);
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
  });
});
