import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import NotificationsPage from "./page";
import {
  fetchAllNotifications,
  markNotificationRead,
} from "@/api/notifications";
import { useRequireAuth } from "@/hooks/useRequireAuth";
import { useRouter } from "next/navigation";
import { toast } from "react-toastify";

jest.mock("@/api/notifications", () => ({
  fetchAllNotifications: jest.fn(),
  markNotificationRead: jest.fn(),
}));

jest.mock("@/hooks/useRequireAuth", () => ({
  useRequireAuth: jest.fn(),
}));

jest.mock("next/navigation", () => ({
  useRouter: jest.fn(),
}));

jest.mock("react-toastify", () => ({
  toast: {
    error: jest.fn(),
  },
}));

jest.mock("@/components/ui/PageLoader", () => ({
  __esModule: true,
  default: ({ message }) => <div data-testid="page-loader">{message || "Loading..."}</div>,
}));

jest.mock("@/components/ui/card", () => ({
  Card: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardHeader: ({ children, ...props }) => <div {...props}>{children}</div>,
  CardTitle: ({ children, ...props }) => <h2 {...props}>{children}</h2>,
  CardDescription: ({ children, ...props }) => <p {...props}>{children}</p>,
  CardContent: ({ children, ...props }) => <div {...props}>{children}</div>,
}));

const mockPush = jest.fn();
const user = { uid: "firebase-uid", email: "tester@example.com" };

const reportCreated = {
  notificationId: 81,
  summary: {
    eventTypeName: "REPORT_CREATED",
    description: "A user submitted a content report",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
    wallSectionId: null,
    problemId: null,
    discussionId: null,
    userId: null,
  },
  createdAt: "2026-08-14T19:11:00Z",
};

const contentRemoved = {
  notificationId: 83,
  summary: {
    eventTypeName: "CONTENT_REMOVED",
    description: "One of your content had been reported and removed.",
  },
  click: {
    kind: "REPORT_QUEUE",
    reportId: 11,
    wallSectionId: null,
    problemId: null,
    discussionId: null,
    userId: null,
  },
  createdAt: "2026-08-18T18:00:00Z",
};

describe("NotificationsPage", () => {
  beforeEach(() => {
    jest.clearAllMocks();
    useRouter.mockReturnValue({ push: mockPush });
    useRequireAuth.mockReturnValue({ ready: true, user, account: { id: 1, roleName: "ADMIN" } });
  });

  it("shows loader while auth is not ready", () => {
    useRequireAuth.mockReturnValue({ ready: false, user: null, account: null });
    render(<NotificationsPage />);
    expect(screen.getByTestId("page-loader")).toBeInTheDocument();
    expect(fetchAllNotifications).not.toHaveBeenCalled();
  });

  it("renders an empty inbox", async () => {
    fetchAllNotifications.mockResolvedValue([]);
    render(<NotificationsPage />);
    expect(await screen.findByText("No notifications.")).toBeInTheDocument();
  });

  it("lists unread and read items", async () => {
    fetchAllNotifications.mockResolvedValue([
      reportCreated,
      { ...contentRemoved, readAt: "2026-08-18T19:00:00Z" },
    ]);
    render(<NotificationsPage />);
    expect(await screen.findByText("New report")).toBeInTheDocument();
    expect(screen.getByText("A user submitted a content report")).toBeInTheDocument();
    expect(screen.getByText("Content removed")).toBeInTheDocument();
    expect(screen.getByText("Unread")).toBeInTheDocument();
    expect(screen.getByText("Read")).toBeInTheDocument();
  });

  it("marks a new-report item read and navigates to the reports queue", async () => {
    fetchAllNotifications.mockResolvedValue([reportCreated]);
    markNotificationRead.mockResolvedValue(undefined);
    render(<NotificationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /New report/i }));
    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith(user, 81);
    });
    expect(mockPush).toHaveBeenCalledWith("/reports?reportId=11");
  });

  it("navigates owner content-removed items to appeal context", async () => {
    fetchAllNotifications.mockResolvedValue([contentRemoved]);
    markNotificationRead.mockResolvedValue(undefined);
    render(<NotificationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /Content removed/i }));
    await waitFor(() => {
      expect(markNotificationRead).toHaveBeenCalledWith(user, 83);
    });
    expect(mockPush).toHaveBeenCalledWith("/appeals?reportId=11");
  });

  it("still navigates when mark-read fails", async () => {
    fetchAllNotifications.mockResolvedValue([reportCreated]);
    markNotificationRead.mockRejectedValue(new Error("Notification not found"));
    render(<NotificationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /New report/i }));
    await waitFor(() => {
      expect(toast.error).toHaveBeenCalled();
    });
    expect(mockPush).toHaveBeenCalledWith("/reports?reportId=11");
  });

  it("navigates already-read items without calling mark-read", async () => {
    fetchAllNotifications.mockResolvedValue([
      { ...reportCreated, readAt: "2026-08-14T20:00:00Z" },
    ]);
    render(<NotificationsPage />);
    fireEvent.click(await screen.findByRole("button", { name: /New report/i }));
    await waitFor(() => {
      expect(mockPush).toHaveBeenCalledWith("/reports?reportId=11");
    });
    expect(markNotificationRead).not.toHaveBeenCalled();
  });

  it("shows an error card when the inbox fails to load", async () => {
    fetchAllNotifications.mockRejectedValue(new Error("Unauthorized"));
    render(<NotificationsPage />);
    expect(await screen.findByText("Error loading notifications")).toBeInTheDocument();
    expect(screen.getByText("Unauthorized")).toBeInTheDocument();
    expect(toast.error).toHaveBeenCalledWith("Failed to load notifications");
  });
});
