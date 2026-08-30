import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import LogbookPage from "./page";
import { fetchAllLogbookEntries, fetchLogbook } from "@/api/moderation";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter, useSearchParams } from "next/navigation";
import { toast } from "react-toastify";
import { downloadTextFile } from "@/lib/moderationLogbook";

jest.mock("@/api/moderation", () => ({
  fetchLogbook: jest.fn(),
  fetchAllLogbookEntries: jest.fn(),
  LOGBOOK_PAGE_SIZE: 25,
}));

jest.mock("@/lib/moderationLogbook", () => {
  const actual = jest.requireActual("@/lib/moderationLogbook");
  return {
    ...actual,
    downloadTextFile: jest.fn(),
  };
});

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
  Dialog: ({ children, open }) => (open ? <div data-testid="logbook-dialog">{children}</div> : null),
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

const dismissedEntry = {
  moderationId: 40,
  decision: "REPORT_DISMISSED",
  adminNote: "Does not violate gym guidelines.",
  createdAt: "2026-08-18T18:05:00Z",
  resolvedBy: { userId: 3, username: "testAdmin", role: "ADMIN" },
  report: {
    targetType: "DISCUSSION",
    discussion: { discussionId: 301, discussionType: "COMMENT", discussionContent: "hello" },
    climbingProblem: { problemId: 100, holdColor: "Red" },
    wallSection: { wallSectionID: 10, wallSectionName: "Cave" },
    reporters: [{ reportId: 11, reporter: { username: "sam" } }],
  },
};

const removedEntry = {
  ...dismissedEntry,
  moderationId: 41,
  decision: "CONTENT_REMOVED",
  adminNote: "Does not belong on this wall.",
  createdAt: "2026-08-17T18:05:00Z",
  resolvedBy: { userId: 3, username: "testAdmin", role: "ADMIN" },
  report: {
    ...dismissedEntry.report,
    reporters: [{ reportId: 12, reporter: { username: "lee" } }],
  },
};

const appealEntry = {
  ...dismissedEntry,
  moderationId: 42,
  decision: "APPEAL_APPROVED",
  adminNote: "Owner explanation accepted.",
  createdAt: "2026-08-19T18:05:00Z",
  report: {
    ...dismissedEntry.report,
    reporters: [{ reportId: 13, reporter: { username: "alex" } }],
  },
};

function renderLogbook({
  ready = true,
  session = adminSession,
  currentUser = user,
  logs = [appealEntry, dismissedEntry, removedEntry],
  moderationId = null,
} = {}) {
  useRequireAuth.mockReturnValue({ ready, user: currentUser, account: session });
  useSearchParams.mockReturnValue({ get: () => moderationId });
  fetchLogbook.mockResolvedValue({ moderationLogs: logs });
  return render(<LogbookPage />);
}

describe("LogbookPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ replace: mockReplace });
    useSearchParams.mockReturnValue({ get: () => null });
  });

  it("shows loader while auth is not ready", () => {
    renderLogbook({ ready: false });
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchLogbook).not.toHaveBeenCalled();
  });

  it("redirects non-admin users", async () => {
    renderLogbook({ session: climberSession, logs: [] });
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
    expect(fetchLogbook).not.toHaveBeenCalled();
  });

  it("renders an empty logbook", async () => {
    renderLogbook({ logs: [] });
    expect(await screen.findByText("No logbook entries.")).toBeInTheDocument();
  });

  it("lists dismiss and remove decisions from the API", async () => {
    renderLogbook();
    expect(await screen.findByText("Dismissed")).toBeInTheDocument();
    expect(screen.getByText("Content removed")).toBeInTheDocument();
    expect(screen.getByText("Appeal approved")).toBeInTheDocument();
    expect(screen.getAllByText(/testAdmin/).length).toBeGreaterThan(0);
    expect(screen.getByText(/report 11/i)).toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Dismiss" })).not.toBeInTheDocument();
    expect(screen.queryByRole("button", { name: "Approve deletion" })).not.toBeInTheDocument();
  });

  it("opens read-only detail with report link and notes", async () => {
    renderLogbook();
    fireEvent.click(await screen.findByRole("button", { name: /Dismissed/i }));
    expect(await screen.findByTestId("logbook-dialog")).toBeInTheDocument();
    expect(
      screen.getAllByText("Does not violate gym guidelines.").length,
    ).toBeGreaterThan(0);
    expect(screen.getByRole("link", { name: "Report 11" })).toHaveAttribute(
      "href",
      "/reports?reportId=11",
    );
    expect(screen.queryByLabelText("Admin notes")).not.toBeInTheDocument();
  });

  it("downloads the logbook as a txt file", async () => {
    fetchAllLogbookEntries.mockResolvedValue([dismissedEntry]);
    renderLogbook();
    fireEvent.click(await screen.findByRole("button", { name: "Download .txt" }));
    await waitFor(() => {
      expect(fetchAllLogbookEntries).toHaveBeenCalledWith(user);
    });
    expect(downloadTextFile).toHaveBeenCalledWith(
      expect.stringMatching(/^v-beta-moderation-logbook-.*\.txt$/),
      expect.stringContaining("Moderation ID: 40"),
    );
    expect(toast.success).toHaveBeenCalledWith("Logbook downloaded.");
  });

  it("redirects when the logbook fetch is access denied", async () => {
    fetchLogbook.mockRejectedValue(new Error("Access denied."));
    useRequireAuth.mockReturnValue({ ready: true, user, account: adminSession });
    useSearchParams.mockReturnValue({ get: () => null });
    render(<LogbookPage />);
    await waitFor(() => {
      expect(mockReplace).toHaveBeenCalledWith("/main-page");
    });
  });
});
